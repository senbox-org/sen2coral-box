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


def spectra_name(base_name, band_name):
    return '{0}:{1}'.format(base_name, band_name)

def check_moreton_bay_data(mbdata, expected_count=11):
    assert isinstance(mbdata, dict)
    assert len(mbdata) == expected_count

    mb_names = ['Zostera muelleri',
                'Halophila ovalis',
                'Halophila spinulosa',
                'Syringodium isoetifolium',
                'Cymodocea serrulata',
                'green algae',
                'brown algae',
                'brown Mud',
                'light brown Mud',
                'white Sand']

    expected_names = [spectra_name('moreton_bay_speclib', x) for x in mb_names]
    for expected_name in expected_names:
        assert expected_name in mbdata

    wavelengths, white_sand = mbdata['moreton_bay_speclib:white Sand']

    assert len(wavelengths) == 600
    assert len(white_sand) == 600
    assert isinstance(wavelengths, np.ndarray)
    assert isinstance(white_sand, np.ndarray)
    assert np.allclose(wavelengths, (range(350, 950, 1)))


class TestSubstrateLoading(object):

    def test_load_envi_spectral_library(self):
        directory = resource_filename(
            sbc.__name__,
            'tests/data/substrates')
        base_name = 'HI_3'

        loaded_substrates = sbc.load_envi_spectral_library(
            directory,
            base_name)

        expected_spectra = [spectra_name(base_name.lower(), x) for x in ['Acropora', 'sand', 'Turf Algae']]
        assert isinstance(loaded_substrates, dict)
        assert len(loaded_substrates) == 3

        for expected_name in expected_spectra:
            assert expected_name in loaded_substrates

        wavelengths, sand = loaded_substrates['hi_3:sand']

        assert len(wavelengths) == 602
        assert len(sand) == 602
        assert isinstance(wavelengths, np.ndarray)
        assert isinstance(sand, np.ndarray)
        assert np.allclose(wavelengths, (range(350, 952, 1)))

    def test_load_whole_directory(self):
        directory = resource_filename(
            sbc.__name__,
            'tests/data/substrates')

        loaded_substrates = sbc.load_all_spectral_libraries(directory)
        assert isinstance(loaded_substrates, dict)

        # we expect 3 spectra in the HI_3 file, 10 in the
        # Moreton_Bay_speclib_final file, and another 11 in the xlsx file
        assert len(loaded_substrates) == 24

        # HI_3 is tested above, lets check some values from the
        # Moreton Bay data ...
        mb_names = ['Zostera muelleri',
                    'Halophila ovalis',
                    'Halophila spinulosa',
                    'Syringodium isoetifolium',
                    'Cymodocea serrulata',
                    'green algae',
                    'brown algae',
                    'brown Mud',
                    'light brown Mud',
                    'white Sand']

        expected_names = [spectra_name('moreton_bay_speclib_final', x) for x in mb_names]
        for expected_name in expected_names:
            assert expected_name in loaded_substrates

        wavelengths, mud = loaded_substrates['moreton_bay_speclib_final:brown Mud']

        assert len(wavelengths) == 600
        assert len(mud) == 600
        assert isinstance(wavelengths, np.ndarray)
        assert isinstance(mud, np.ndarray)
        assert np.allclose(wavelengths, (range(350, 950, 1)))

    def test_load_missing_file(self):
        directory = resource_filename(
            sbc.__name__,
            'tests/data/substrates')
        base_name = 'missing_file'

        with pytest.raises(IOError):
            sbc.load_envi_spectral_library(directory, base_name)

    def test_load_excel(self):
        filename = resource_filename(
            sbc.__name__,
            'tests/data/substrates/Moreton_Bay_speclib.xlsx')

        loaded_substrates = sbc.load_excel_spectral_library(filename)
        check_moreton_bay_data(loaded_substrates)

        # This extra spectra isn't tested in check_moreton_bay_data,
        # as it isn't present in the csv version
        assert 'moreton_bay_speclib:weird_substrate' in loaded_substrates

    def test_load_csv(self):
        filename = resource_filename(
            sbc.__name__,
            'tests/data/substrates/Moreton_Bay_speclib.csv')

        loaded_substrates = sbc.load_csv_spectral_library(filename)
        check_moreton_bay_data(loaded_substrates, expected_count=10)


class TestMagicFileDetectionLoading(object):
    def test_csv(self):
        filename = resource_filename(
            sbc.__name__,
            'tests/data/siop/aw_340_900_lw2002_1nm.csv')

        data = sbc.load_spectral_library(filename)
        assert isinstance(data, dict)
        assert len(data) == 1

        wavs, values = data['aw_340_900_lw2002_1nm:awater']
        assert len(wavs) == 562
        assert min(wavs) == 340
        assert max(wavs) == 901
        assert sbc.strictly_increasing(wavs)

    def test_excel(self):
        filename = resource_filename(
            sbc.__name__,
            'tests/data/siop/aw_340_900_lw2002_1nm.xlsx')

        data = sbc.load_spectral_library(filename)
        assert isinstance(data, dict)
        assert len(data) == 1

        wavs, values = data['aw_340_900_lw2002_1nm:awater']
        assert len(wavs) == 562
        assert min(wavs) == 340
        assert max(wavs) == 901
        assert sbc.strictly_increasing(wavs)

    def test_envi_hdr_extension(self):
        filename = resource_filename(
            sbc.__name__,
            'tests/data/siop/WL_aphy_1nm.hdr')
        data = sbc.load_spectral_library(filename)
        assert isinstance(data, dict)
        assert len(data) == 1

    def test_missing(self):
        filename = resource_filename(
            sbc.__name__,
            'tests/data/siop/missing_file')

        with pytest.raises(IOError):
            sbc.load_spectral_library(filename)

class TestValidationFlag(object):
    def test_invalid_file_fails(self):
        filename = resource_filename(
            sbc.__name__,
            'tests/data/nedr/fails_validation.hdr')

        with pytest.raises(sbc.DataValidationError):
            sbc.load_spectral_library(filename, validate=True)

    def test_invalid_file_loads(self):
        filename = resource_filename(
            sbc.__name__,
            'tests/data/nedr/fails_validation.hdr')

        data = sbc.load_spectral_library(filename, validate=False)
        assert isinstance(data, dict)
        assert len(data) == 1
