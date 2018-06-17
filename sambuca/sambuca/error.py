""" Sambuca Error Functions.

    Used when assessing model closure during parameter estimation.
"""


from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)
from builtins import *

from collections import namedtuple

import numpy as np

ErrorTerms = namedtuple('ErrorTerms',
                        [
                            'alpha',
                            'alpha_f',
                            'f',
                            'lsq',
                        ])
# TODO: update error attribute docstrings
""" namedtuple containing the error terms.

Attributes:
    alpha (float): TODO
    alpha_f (float): TODO
    f (float): TODO
    lsq (float): TODO
"""


# pylint generates no-member warnings for valid named tuple members
# pylint: disable=no-member

def error_all(observed_rrs, modelled_rrs, nedr=None):
    """Calculates all common error terms.

    Args:
        observed_rrs (array-like): The observed reflectance(remotely-sensed).
        modelled_rrs (array-like): The modelled reflectance(remotely-sensed).
        nedr (array-like): Noise equivalent difference in reflectance.

    Returns:
        ErrorTerms: The error terms.
    """
   
    # LSQ as in as in equation 1 of Mobley 2005 AO:i.e. without using Noise
    # L^2 Vector Norm of observed - modelled
    lsq = np.linalg.norm(observed_rrs - modelled_rrs)

    if nedr is not None:
        # deliberately avoiding an in-place divide as I want a copy of the
        # spectra to avoid side-effects due to pass by reference semantics
        observed_rrs = observed_rrs / nedr
        modelled_rrs = modelled_rrs / nedr

    f_val = np.linalg.norm(observed_rrs - modelled_rrs) / observed_rrs.sum()

    # TODO: Determine a good value for the lower bound of rat_denom
    rat_numerator = np.sum(observed_rrs * modelled_rrs)
    rat_denom = np.linalg.norm(observed_rrs) * np.linalg.norm(modelled_rrs)
    rat_denom = np.clip(rat_denom, 1e-9, rat_denom)
    rat = rat_numerator / rat_denom
    rat = np.clip(rat, 0.0, 1.0)

    alpha_val = np.arccos(rat)

    return ErrorTerms(alpha_val, alpha_val * f_val, f_val, lsq)


    
def distance_alpha(observed_rrs, modelled_rrs, nedr=None):
    # TODO: complete the docstring
    """Calculates TODO

    Args:
        observed_rrs: The observed reflectance(remotely-sensed).
        modelled_rrs: The modelled reflectance(remotely-sensed).
        noise: Optional spectral noise values.

    Returns: TODO
    """
    return error_all(observed_rrs, modelled_rrs, nedr).alpha


def distance_alpha_f(observed_rrs, modelled_rrs, nedr=None):
    # TODO: complete the description
    """Calculates TODO

    Args:
        observed_rrs: The observed reflectance(remotely-sensed).
        modelled_rrs: The modelled reflectance(remotely-sensed).
        noise: Optional spectral noise values.

    Returns: TODO
    """
    return error_all(observed_rrs, modelled_rrs, nedr).alpha_f


def distance_lsq(observed_rrs, modelled_rrs, nedr=None):
    # TODO: complete the description
    """Calculates TODO

    Args:
        observed_rrs: The observed reflectance(remotely-sensed).
        modelled_rrs: The modelled reflectance(remotely-sensed).
        noise: Optional spectral noise values.

    Returns: TODO
    """
    return error_all(observed_rrs, modelled_rrs, nedr).lsq


def distance_f(observed_rrs, modelled_rrs, nedr=None):
    # TODO: complete the description
    """Calculates TODO

    Args:
        observed_rrs: The observed reflectance(remotely-sensed).
        modelled_rrs: The modelled reflectance(remotely-sensed).
        noise: Optional spectral noise values.

    Returns: TODO
    """
    return error_all(observed_rrs, modelled_rrs, nedr).f

    