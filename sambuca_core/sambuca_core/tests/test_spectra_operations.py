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


def test_spectra_find_common_wavelengths_no_intersection_returns_empty_array():
    a = ([1,2,3,4,5], [10,20,30,40,50])
    b = ([25,26,27], [6,13,9])
    c = ([26,27,28], [11,42,8])
    mask = sbc.spectra_find_common_wavelengths(a, b, c)
    assert len(mask) == 0

def test_spectra_find_common_wavelengths_no_args():
    with pytest.raises(ValueError):
        sbc.spectra_find_common_wavelengths()

def test_spectra_find_common_wavelengths_4_spectra():
    # build 4 fake spectra tuples (wavelengths, values)
    a = ([1,2,3,4,5], [10,20,30,40,50])
    b = ([2,3,4], [6,13,9])
    c = ([3,4,5], [11,42,8])
    d = ([1,2,3,4], [19,7,73,13])
    mask = sbc.spectra_find_common_wavelengths(a, b, c, d)
    assert len(mask) == 2
    assert np.allclose(mask, [3,4])

def test_spectra_find_common_wavelengths():
    one = ([1,2,3,4,5], [10,20,30,40,50])
    two = ([2,3,4], [2,3,4])
    mask = sbc.spectra_find_common_wavelengths(one[0], two[0])
    assert len(mask) == 3
    assert np.allclose(mask, [2,3,4])

def test_mask_spectra_wavelengths():
    one = (np.asarray([1,2,3,4,5]), np.asarray([10,20,30,40,50]))
    two = (np.asarray([2,3,4]), np.asarray([2,3,4]))
    mask = sbc.spectra_find_common_wavelengths(one, two)

    masked_wavs, masked_values = sbc.spectra_apply_wavelength_mask(one, mask)
    assert len(masked_wavs) == len(mask)
    assert len(masked_values) == len(mask)
    assert np.allclose(masked_wavs, [2,3,4])
    assert np.allclose(masked_values, [20, 30, 40])

def test_spectra_find_common_wavelengths_called_with_tuple():
    one = ([1,2,3,4,5], [10,20,30,40,50])
    two = ([2,3,4], [2,3,4])
    mask_wavs = sbc.spectra_find_common_wavelengths(one[0], two[0])
    mask_tuple = sbc.spectra_find_common_wavelengths(one, two)

    assert np.allclose(mask_wavs, mask_tuple)

def test_mask_spectra_wavelengths_actual_data():
    filename = resource_filename(
        sbc.__name__,
        'tests/data/siop/aw_340_900_lw2002_1nm.csv')
    # load_spectral_library returns a dictionary. Popitem simply extracts the
    # only value. We don't care about the key in this instance.
    one = sbc.load_spectral_library(filename).popitem()[1]
    one_wavs, one_values = one[0], one[1]
    assert one_wavs.min() == 340
    assert one_wavs.max() == 901

    filename = resource_filename(
        sbc.__name__,
        'tests/data/siop/WL_aphy_1nm.hdr')
    two = sbc.load_spectral_library(filename).popitem()[1]
    two_wavs, two_values = two[0], two[1]
    assert two_wavs.min() == 350
    assert two_wavs.max() == 800

    # generate the mask
    mask = sbc.spectra_find_common_wavelengths(one, two)
    assert len(mask) == len(two_wavs)

    # mask the larger spectra to the smaller set of wavelengths
    masked_wavs, masked_values = sbc.spectra_apply_wavelength_mask(one, mask)
    assert masked_wavs.min() == mask[0]
    assert masked_wavs.max() == mask[-1]
    assert len(masked_wavs) == len(mask)
    assert len(masked_values) == len(mask)



