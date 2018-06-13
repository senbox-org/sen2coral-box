""" Sambuca Free Parameters

    Defines the default set of free parameters for use with the
    default parameter estimation function.
"""


from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)
from builtins import *

from collections import namedtuple


FreeParameters = namedtuple('FreeParameters',
                            '''
                                chl,
                                cdom,
                                nap,
                                depth,
                                substrate_fraction
                            ''')
""" namedtuple containing the default Sambuca free parameters.

Attributes:
    chl (float): Concentration of chlorophyll (algal organic particulates).
    cdom (float): Concentration of coloured dissolved organic particulates
        (CDOM).
    nap (float): Concentration of non-algal particulates (NAP).
    depth (float): Water column depth.
    substrate_fraction (float): relative proportion of substrate1
        and substrate2.
"""
