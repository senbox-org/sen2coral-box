# -*- coding: utf-8 -*-
# Ensure compatibility of Python 2 with Python 3 constructs
from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)

from os.path import basename, splitext

import numpy as np
import pytest
from pkg_resources import resource_filename

import sambuca_core as sbc


class TestExcelSensorFilterLoading(object):
    """ Sensor filter loading tests. """

    def test_xls_format(self):
        file = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters/sensor_filters.xls')
        loaded_filters = sbc.load_sensor_filters_excel(file)
        assert len(loaded_filters) == 1

    def test_unknown_worksheet_doesnt_throw(self):
        file = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters/sensor_filters.xlsx')
        sbc.load_sensor_filters_excel(
            file,
            sheet_names=['non_existant'])

    def test_load_single_worksheet(self):
        file = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters/sensor_filters.xlsx')
        expected_name = '3_band_350_900'
        loaded_filters = sbc.load_sensor_filters_excel(
            file,
            normalise=False,
            sheet_names=[expected_name])

        assert len(loaded_filters) == 1
        assert isinstance(loaded_filters, dict)
        assert expected_name in loaded_filters

    def test_load_all_worksheets(self):
        file = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters/sensor_filters.xlsx')
        loaded_filters = sbc.load_sensor_filters_excel(file)
        assert len(loaded_filters) == 3

    def test_load_multiple_skips_invalid_and_missing_sheets(self):
        file = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters/sensor_filters.xlsx')
        good_names = ['3_band_350_900', '5_band_400_800']
        missing_names = ['Monty_Hall', 'Monty_Python']
        invalid_names = ['Deliberately_Invalid', 'wavelengths_out_of_sequence']
        names = good_names + missing_names + invalid_names
        loaded_filters = sbc.load_sensor_filters_excel(
            file,
            normalise=False,
            sheet_names=names)

        assert len(loaded_filters) == len(good_names)

    def test_normalise(self):
        file = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters/sensor_filters.xlsx')
        expected_name = '3_band_350_900'
        loaded_filters = sbc.load_sensor_filters_excel(
            file,
            normalise=True,
            sheet_names=[expected_name])
        actual_filter = loaded_filters[expected_name][1]

        assert np.allclose(actual_filter[0, ].max(), 1.0)
        assert np.allclose(actual_filter[1, ].max(), 1.0)
        assert np.allclose(actual_filter[2, ].max(), 1.0)

    def test_valid_worksheet(self):
        file = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters/sensor_filters.xlsx')
        expected_name = '3_band_350_900'
        loaded_filters = sbc.load_sensor_filters_excel(
            file,
            normalise=False,
            sheet_names=[expected_name])
        actual_filter = loaded_filters[expected_name][1]

        assert isinstance(actual_filter, np.ndarray)
        assert actual_filter.shape == (3, 551)
        assert np.allclose(actual_filter[0, ].min(), 0.43)
        assert np.allclose(actual_filter[0, ].max(), 4.3)
        assert np.allclose(actual_filter[1, ].min(), 0.1)
        assert np.allclose(actual_filter[1, ].max(), 2.39)
        assert np.allclose(actual_filter[2, ].min(), 0.9)
        assert np.allclose(actual_filter[2, ].max(), 11.0)

    def test_undersampled_worksheet(self):
        """ Current functionality is to skip any sensor filters that are not
        already specified at exact 1nm bands.
        When interpolation is implemented, this test will need to change to
        reflect the new expectations.
        """
        # TODO: update test when band interpolation is implemented
        file = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters/sensor_filters.xlsx')
        expected_name = '10nm_3_bands_350_900'
        loaded_filters = sbc.load_sensor_filters_excel(
            file,
            normalise=False,
            sheet_names=[expected_name])

        assert len(loaded_filters) == 0

    def test_oversampled_worksheet(self):
        """ Current functionality is to skip any sensor filters that are not
        already specified at exact 1nm bands.
        When band-averaging is implemented, this test will need to change to
        reflect the new expectations.
        """
        # TODO: update test when band averaging is implemented
        file = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters/sensor_filters.xlsx')
        expected_name = '0.1nm_3_bands_350_355'
        loaded_filters = sbc.load_sensor_filters_excel(
            file,
            normalise=False,
            sheet_names=[expected_name])

        assert len(loaded_filters) == 0

    def test_wavelengths(self):
        file = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters/sensor_filters.xlsx')
        expected_names = ['3_band_350_900', '5_band_400_800', '4_band_300_1000']
        loaded_filters = sbc.load_sensor_filters_excel(
            file,
            normalise=False,
            sheet_names=expected_names)
        expected_wavelengths = {
            expected_names[0]: (range(350, 901, 1)),
            expected_names[1]: (range(400, 801, 1)),
            expected_names[2]: (range(300, 1001, 1))}

        assert len(loaded_filters) == len(expected_names)

        for name in expected_names:
            expected = expected_wavelengths[name]
            actual = loaded_filters[name][0]
            assert len(expected) == len(actual)
            assert np.allclose(expected, actual)
            assert isinstance(actual, np.ndarray)


class TestSpectralLibrarySensorFilterLoading(object):

    def test_valid_load_casi(self):
        directory = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters')
        base_name = 'CASI04_350_900_1nm'

        wavelengths, sensor_filter = sbc.load_sensor_filter_spectral_library(
            directory,
            base_name,
            normalise=False)

        assert len(wavelengths) == 551
        assert isinstance(wavelengths, np.ndarray)
        assert isinstance(sensor_filter, np.ndarray)
        assert sensor_filter.shape == (30, 551)
        assert np.allclose(wavelengths, (range(350, 901, 1)))

    def test_load_missing_file(self):
        directory = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters')
        base_name = 'missing_file'

        with pytest.raises(IOError):
            sbc.load_sensor_filter_spectral_library(directory, base_name)


class TestLoadAllSensorFilters(object):

    def test_invalid_directory(self):
        directory = resource_filename(
            sbc.__name__,
            'tests/data/missing_stuff')
        with pytest.raises(OSError):
            sbc.load_sensor_filters(directory)

    def test_filter_names(self):
        expected_names = [
            'casi04',  # Spectral Library
            'qbtest',  # Spectral Library
            '3_band_350_900',  # XLSX
            '4_band_300_1000',  # XLSX
            '5_band_400_800',  # XLSX
            '4_band_300_1000_xls',  # XLS
        ]
        all_filters = self.load_all_filters()
        for name in expected_names:
            assert name in all_filters

    def test_filter_counts(self):
        all_filters = self.load_all_filters()
        # I expect 6 filters:
        # 3 valid filters in the xlsx
        # 1 in the xls
        # 2 spectral libraries (casi and quickbird)
        assert len(all_filters) == 6

    def load_all_filters(self):
        directory = resource_filename(
            sbc.__name__,
            'tests/data/sensor_filters')
        # Nasty name parser based on the test filters having the pattern
        # XXXX_350_900_1nm.lib where XXXX is the name
        return sbc.load_sensor_filters(
            directory,
            normalise=False,
            spectral_library_name_parser=lambda path:
            splitext(basename(path).lower())[0].split('_')[0])
