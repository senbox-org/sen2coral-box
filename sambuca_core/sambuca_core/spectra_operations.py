# -*- coding: utf-8 -*-
""" Contains functions for manipulating the (wavelength, value) tuples returned
by the spectra readers.
"""

from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)
from builtins import *

import numpy as np


def spectra_find_common_wavelengths(*args):
    """ Finds the common subset of wavelengths for the given inputs.
    I could have called this intersect, but chose the name based on purpose.

    Args:
        *args (array-like): a vector of wavelength values, or a
            (wavelength, values) tuple.

    Returns:
        numpy.ndarray: The common subset of wavelengths, which can be used as an
            input to spectra_apply_wavelength_mask.
    """

    if args:
        common = args[0]
        for a in args[1:]:
            common = np.intersect1d(common, a)

        return common
    else:
        raise ValueError('Invalid or insufficient arguments')


def spectra_apply_wavelength_mask(spectra, mask):
    """ Applies a wavelength mask to a spectra ((wavelengths, values) tuple).
    All values in the spectra that are not in the mask will be removed in the
    returned values. The input spectra is not modified.

    Args:
        spectra (tuple): the (wavelengths, values) spectra tuple.
        mask (array-like): The wavelength values that should be retained.

    Returns:
        The masked tuple of (wavelengths, values).
    """

    boolean_mask = (spectra[0] >= mask.min()) & (spectra[0] <= mask.max())
    return spectra[0][boolean_mask], spectra[1][boolean_mask]
