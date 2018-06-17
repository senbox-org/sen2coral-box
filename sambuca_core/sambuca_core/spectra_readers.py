# -*- coding: utf-8 -*-
""" Contains functions for loading collections of spectra from Sambuca
spectral database directories. """

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

def _validate_spectra_dataframe(spectra_dataframe):
    """ Internal function to validate a spectra data frame.

    Args:
        spectra_dataframe (pandas.DataFrame): the

    Returns:
        bool: True if the spectra is valid; otherwise false.
    """

    wavelengths = spectra_dataframe.index

    # are the band-centre wavelengths strictly increasing?
    if not strictly_increasing(wavelengths):
        return False

    # Are the wavelength spacings acceptable?
    # For now, only spectra that are specified with exact
    # 1nm bands are supported.
    band_diffs = np.ediff1d(wavelengths)
    if band_diffs.min() < 1.0 or band_diffs.max() > 1.0:
        # TODO: log warning about interpolation/averaging not being supported
        return False

    # The dtype of every column needs to be a numpy-compatible number
    if len(spectra_dataframe.select_dtypes(include=[np.number]).columns) \
       != len(spectra_dataframe.columns):
        return False

    return True


def _add_dataframe_spectra_to_dictionary(dataframe, base_name, dictionary=None):
    """ Adds all spectra from a dataframe to a dictionary, building the spectra
    name as 'base_name:column_name'
    """
    dictionary = {} if not dictionary else dictionary
    for column in dataframe:
        dictionary['{0}:{1}'.format(base_name.lower(), column)] = (
            np.array(dataframe.index),
            dataframe[column].values)
    return dictionary


def load_csv_spectral_library(filename, validate=True):
    """ Loads a spectral library from a CSV file.
    The CSV file must have a header row, and the wavelengths must be the first
    column.

    Args:
        filename (str): full path to the Excel file.
        validate (bool): If true, data validation will be performed.

    Returns:
       dict: A dictionary of 2-tuples of numpy.ndarrays.
            The first element contains the band centre wavelengths,
            while the second element contains the spectra.
            The dictionary is keyed by spectra name, formed by concatenation
            of the file and band names. This allows multiple spectra from
            multiple files to be unambigiously collected into a dictionary.
            Note that the filename component is always converted to lower case.
            This is required for consistent results on Linux and Windows.
    """
    dataframe = pd.read_csv(filename, index_col=0)
    if validate and not _validate_spectra_dataframe(dataframe):
        raise DataValidationError('{0} failed validation'.format(filename))

    # if normalise:
    #     dataframe = _normalise_dataframe(dataframe)

    base_name, _ = os.path.splitext(os.path.basename(filename))
    return _add_dataframe_spectra_to_dictionary(dataframe, base_name)

def load_excel_spectral_library(filename, sheet_names=None, validate=True):
    """ Loads a spectral library from an Excel file. Both new style XLSX and
    old-style XLS formats are supported.

    Args:
        filename (str): full path to the Excel file.
        sheet_names (list): Optional list of worksheet names to load.
            The default is to attempt to load all worksheets.
        validate (bool): If true, data validation will be performed.

    Returns:
       dict: A dictionary of 2-tuples of numpy.ndarrays.
            The first element contains the band centre wavelengths,
            while the second element contains the spectra.
            The dictionary is keyed by spectra name, formed by concatenation
            of the file and band names. This allows multiple spectra from
            multiple files to be unambigiously collected into a dictionary.
            Note that the filename component is always converted to lower case.
            This is required for consistent results on Linux and Windows.
    """
    all_spectra = {}
    with pd.ExcelFile(filename) as excel_file:
        base_name, _ = os.path.splitext(os.path.basename(filename))
        # default is all sheets
        if not sheet_names:
            sheet_names = excel_file.sheet_names

        for sheet in sheet_names:
            try:
                dataframe = excel_file.parse(sheet)  # the sheet as a DataFrame
                # OK, we have the data frame. Let's process it...
                if validate and not _validate_spectra_dataframe(dataframe):
                    continue

                # if normalise:
                #     dataframe = _normalise_dataframe(dataframe)

                all_spectra = _add_dataframe_spectra_to_dictionary(
                    dataframe,
                    base_name,
                    all_spectra)
            except xlrd.biffh.XLRDError:
                continue
            # except xlrd.biffh.XLRDError as xlrd_error:
                # TODO: log warning about invalid sheet

    return all_spectra


def load_envi_spectral_library(
        directory,
        base_filename,
        validate=True):
    """ Loads spectra from an ENVI spectral library.

    Args:
        directory (str): Directory containing the spectral library file.
        base_filename (str): The filename without the extension or '.'
            preceeding the extension.
        validate (bool): If true, data validation will be performed.

    Returns:
        dict: A dictionary of 2-tuples of numpy.ndarrays.
            The first element contains the band centre wavelengths,
            while the second element contains the spectra.
            The dictionary is keyed by spectra name, formed by concatenation
            of the file and band names. This allows multiple spectra from
            multiple files to be unambigiously collected into a dictionary.
            Note that the filename component is always converted to lower case.
            This is required for consistent results on Linux and Windows.
    """

    full_filename = os.path.join(directory, base_filename)
    file_pattern = '{0}.{1}'

    # load the spectral library
    try:
        spectral_library = envi.open(
            file_pattern.format(full_filename, 'hdr'),
            file_pattern.format(full_filename, 'lib'))
    except spyfile.FileNotFoundError as exception:
        raise IOError(exception)

    # convert to a DataFrame for processing
    dataframe = pd.DataFrame(
        spectral_library.spectra.transpose(),
        index=spectral_library.bands.centers)
    dataframe.columns = spectral_library.names

    if validate and not _validate_spectra_dataframe(dataframe):
        raise DataValidationError(
            'Spectral library {0} failed validation'.format(
                base_filename))

    # merge the spectra into a dictionary
    return _add_dataframe_spectra_to_dictionary(dataframe, base_filename)


def load_all_spectral_libraries(path, validate=True):
    """ Loads all valid spectra from the given location.

    Args:
        path (str): The directory path to scan for supported spectra files.
        validate (bool): If true, data validation will be performed.

    Returns:
        dict: A dictionary of 2-tuples of numpy.ndarrays.
            The first element contains the band centre wavelengths of the input
            bands, while the second element contains the spectra values.
            Dictionary is keyed by spectra name built from the file and
            band/sheet names, separated by a colon.

            Note that names are not disambiguated, so that if more than one
            filter has the same name, only the first will be returned and no
            error will be raised (although it will be logged).

            Note that the filename component is always converted to lower case.
            This is required for consistent results on Linux and Windows.
    """
    # TODO: add logging
    # logging.getLogger(__name__).info(
    #     'Loading Sensor filters from %s', path)

    all_spectra = {}
    new_spectra = {}

    # excel files
    for file in list_files(path, ['xls', 'xlsx']):
        try:
            new_spectra = load_excel_spectral_library(file, validate=validate)
        except UnsupportedDataFormatError:
            pass
            # except UnsupportedDataFormatError as ex:
            # logging.getLogger(__name__).exception(ex)
            # TODO: logging
        merge_dictionary(all_spectra, new_spectra)

    # CSV files
    for file in list_files(path, ['csv']):
        try:
            new_spectra = load_csv_spectral_library(file, validate=validate)
        except UnsupportedDataFormatError:
            pass
            # except UnsupportedDataFormatError as ex:
            # logging.getLogger(__name__).exception(ex)
            # TODO: logging
        merge_dictionary(all_spectra, new_spectra)

    # Spectral Libraries
    for file in list_files(path, ['lib']):
        try:
            base_name, _ = os.path.splitext(os.path.basename(file))
            new_spectra = load_envi_spectral_library(
                path,
                base_name,
                validate=validate)
        except UnsupportedDataFormatError:
            pass
            # except UnsupportedDataFormatError as ex:
            # TODO: logging.getLogger(__name__).exception(ex)
        merge_dictionary(all_spectra, new_spectra)

    return all_spectra


def load_spectral_library(filename, validate=True):
    """ Loads a single spectral library from the given file name from any
    supported format (selected by file extension).

    Args:
        filename (str): full path to the file.
        validate (bool): If true, data validation will be performed.

    Returns:
        dict: A dictionary of 2-tuples of numpy.ndarrays.
            The first element contains the band centre wavelengths of the input
            bands, while the second element contains the spectra values.
            Dictionary is keyed by spectra name built from the file and
            band/sheet names, separated by a colon.
            For example: ``Moreton_Bay_speclib:white_sand``

            Note that names are not disambiguated, so that if more than one
            filter has the same name, only the first will be returned and no
            error will be raised (although it will be logged).

            Note that the filename component is always converted to lower case.
            This is required for consistent results on Linux and Windows.
    """
    # TODO: add logging
    # logging.getLogger(__name__).info(
    #     'Loading Sensor filters from %s', path)

    if not os.path.isfile(filename):
        raise IOError(filename)

    base_name, extension = os.path.splitext(os.path.basename(filename))
    extension = extension[1:].lower()

    # excel
    if extension in ['xls', 'xlsx']:
        return load_excel_spectral_library(filename, validate=validate)
    # CSV
    if extension in ['csv']:
        return load_csv_spectral_library(filename, validate=validate)
    # ENVI Spectral Libraries
    elif extension in ['hdr', 'lib']:
        return load_envi_spectral_library(os.path.dirname(filename),
                                          base_name,
                                          validate=validate)

    raise UnsupportedDataFormatError(
        'filename {0} is not a supported format'.format(filename))
