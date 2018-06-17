# -*- coding: utf-8 -*-
""" Numpy-specific utility functions. """

# Disable some pylint warnings caused by future and tkinter
# pylint: disable=unused-wildcard-import
# pylint: disable=redefined-builtin
# pylint: disable=wildcard-import
# pylint: disable=too-many-ancestors

# Ensure backwards compatibility with Python 2
from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals,
)
from builtins import *

import numpy as np


# In this instance, I think that x is a good name.
# pylint: disable=invalid-name
def strictly_increasing(x):
    """ Tests if a 1D vector is strictly increasing, where
    x[i+1] > x[i] for i in [0 .. len(x)].

    Args:
        x (array-like): The vector to test.

    Returns:
        bool: True if x is strictly increasing; false otherwise.
    """
    diffs = np.ediff1d(x)
    return np.all(diffs > 0)

def strictly_decreasing(x):
    """ Tests if a 1D vector is strictly decreasing, where
    x[i+1] < x[i] for i in [0 .. len(x)].

    Args:
        x (array-like): The vector to test.

    Returns:
        bool: True if x is strictly decreasing; false otherwise.
    """
    diffs = np.ediff1d(x)
    return np.all(diffs < 0)
# pylint: enable=invalid-name
