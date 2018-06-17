# -*- coding: utf-8 -*-
""" Contains functions for working with Sensor Filters. """

from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)
from builtins import *

import os

import numpy as np
import pandas as pd
import spectral.io.envi as envi
import spectral.io.spyfile as spyfile
import xlrd

from .exceptions import UnsupportedDataFormatError, DataValidationError
from .utility import list_files, strictly_increasing, merge_dictionary


def apply_sensor_filter(spectra, normalised_response_function):
    """Applies a sensor filter to a spectra using the given spectral
    response function.

    Args:
        spectra (array-like): The input spectra.
        normalised_response_function (matrix-like): The spectral sensitivity
            matrix.
            The first dimension determines the number of output bands.
            The second dimension represents the proportional contribution of
            each of the input bands to an output band. The size must match the
            number of bands in the input spectra.

    Returns:
        ndarray: The filtered spectra.

    """

    return np.dot(
        normalised_response_function,
        spectra) / normalised_response_function.sum(1)

def _validate_filter_dataframe(filter_dataframe):
    """ Internal function to validate a sensor filter data frame.

    Args:
        filter_dataframe (pandas.DataFrame): the sensor filter

    Returns:
        bool: True if the filter is valid; otherwise false.
    """

    wavelengths = filter_dataframe.index

    # are the band-centre wavelengths strictly increasing?
    if not strictly_increasing(wavelengths):
        return False

    # Are the wavelength spacings acceptable?
    # For now, only sensor filters that are specified with exact
    # 1nm bands are supported.
    band_diffs = np.ediff1d(wavelengths)
    if band_diffs.min() < 1.0 or band_diffs.max() > 1.0:
        # TODO: log warning about interpolation/averaging not being supported
        return False

    # The dtype of every column needs to be a numpy-compatible number
    if len(filter_dataframe.select_dtypes(include=[np.number]).columns) \
       != len(filter_dataframe.columns):
        return False

    return True

def _normalise_dataframe(dataframe):
    """ Normalises the spectral bands in a dataframe.

    Args:
        dataframe (pandas.DataFrame): the spectral data.

    Returns:
        pandas.DataFrame: The normalised spectral data.
    """

    # normalise all bands relative to the strongest
    # as this preserves the relative band strengths
    # return dataframe / max(dataframe.max())

    # per-band normalisation
    return dataframe / dataframe.max()

def load_sensor_filter_spectral_library(
        directory,
        base_filename,
        normalise=False):
    """ Loads a single sensor filter from an ENVI spectral library.

    Args:
        directory (str): Directory containing the sensor filter file.
        base_filename (str): The filename without the extension or '.'
            preceeding the extension.
        normalise (bool): If true, the filter will be normalised.

    Returns:
        numpy.array: The band-centre wavelengths.
        numpy.array: The sensor filter.
    """

    base_filename = os.path.join(directory, base_filename)
    file_pattern = '{0}.{1}'

    # load the spectral library
    try:
        spectral_library = envi.open(
            file_pattern.format(base_filename, 'hdr'),
            file_pattern.format(base_filename, 'lib'))
    except spyfile.FileNotFoundError as exception:
        raise IOError(exception)

    # convert to a DataFrame
    dataframe = pd.DataFrame(
        spectral_library.spectra.transpose(),
        index=spectral_library.bands.centers)
    dataframe.columns = ['Band {0}'.format(x+1)
                         for x in range(len(dataframe.columns))]

    if not _validate_filter_dataframe(dataframe):
        raise DataValidationError(
            'Spectral library {0} failed validation'.format(
                base_filename))

    if normalise:
        dataframe = _normalise_dataframe(dataframe)

    return np.array(dataframe.index), dataframe.values.transpose()

# TODO: option to clip the filters to a specific range of 1nm bands?
def load_sensor_filters_excel(filename, normalise=False, sheet_names=None):
    """ Loads sensor filters from an Excel file. Both new style XLSX and
    old-style XLS formats are supported.

    Args:
        filename (str): full path to the Excel file.
        normalise (boolean): Determines whether the filter bands will be
            normalised after loading.
        sheet_names (list): Optional list of worksheet names to load.
            The default is to attempt to load all worksheets.

    Returns:
        dict: A dictionary of 2-tuples of numpy.ndarrays.
            The first element contains the band centre wavelengths of the input
            bands, while the second element contains the filter.
            Dictionary is keyed by filter name inferred from the sheet name.
    """

    sensor_filters = {}
    with pd.ExcelFile(filename) as excel_file:
        # default is all sheets
        if not sheet_names:
            sheet_names = excel_file.sheet_names

        for sheet in sheet_names:
            try:
                dataframe = excel_file.parse(sheet)  # the sheet as a DataFrame
                # OK, we have the data frame. Let's process it...
                if not _validate_filter_dataframe(dataframe):
                    continue

                if normalise:
                    dataframe = _normalise_dataframe(dataframe)

                sensor_filters[sheet] = (
                    np.array(dataframe.index),
                    dataframe.values.transpose())

            except xlrd.biffh.XLRDError:
                continue
            # except xlrd.biffh.XLRDError as xlrd_error:
                # TODO: log warning about invalid sheet

    return sensor_filters


def load_sensor_filters(
        path,
        normalise=False,
        spectral_library_name_parser=None):
    """" Loads all valid sensor filters from the given location.

    Args:
        path (str): The directory path to scan for sensor filters.
        normalise (boolean): Determines whether the filter bands will be
            normalised after loading.
        spectral_library_name_parser (function): If supplied, this function
            accepts a single string argument (the full path to a spectral
            library file) and returns the sensor filter name that will be used
            in the dictionary of results.

    Returns:
        dict: A dictionary of 2-tuples of numpy.ndarrays.
            The first element contains the band centre wavelengths of the input
            bands, while the second element contains the filter.
            Dictionary is keyed by filter name inferred from the sheet name.

            Note that names are not disambiguated, so that if more than one
            filter has the same name, only the first will be returned and no
            error will be raised (although it will be logged).
    """
    # TODO: add logging
    # logging.getLogger(__name__).info(
    #     'Loading Sensor filters from %s', path)

    sensor_filters = {}
    new_filters = {}

    try:
        # excel files
        for file in list_files(path, ['xls', 'xlsx']):
            new_filters = load_sensor_filters_excel(file, normalise=normalise)
            merge_dictionary(sensor_filters, new_filters)

        # Spectral Libraries
        for file in list_files(path, ['lib']):
            base_name, _ = os.path.splitext(os.path.basename(file))

            if spectral_library_name_parser:
                name = spectral_library_name_parser(file)
            else:
                name = base_name

            loaded_filter = load_sensor_filter_spectral_library(
                path,
                base_name,
                normalise=normalise)

            if name not in sensor_filters:
                sensor_filters[name] = loaded_filter
    except UnsupportedDataFormatError:
        pass
    # except UnsupportedDataFormatError as ex:
        # TODO: logging.getLogger(__name__).exception(ex)

    return sensor_filters
