""" Pixel Result Handler
"""


from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)
from builtins import *

from collections.abc import Callable


class PixelResultHandler(Callable):
    """ Pixel result handler base class.

    Pixel result handlers are called by the parameter estimator with the final
    result for a pixel. Subclasses can implement various functionality, such as
    writing results to memory structures or files.
    """

    def __init__(self):
        pass

    def __call__(self, x, y, observed_rrs, parameters=None):
        """
        Called by the parameter estimator when there is a result for a pixel.

        Args:
            x (int): The pixel x coordinate.
            y (int): The pixel y coordinate.
            observed_rrs (array-like): The observed remotely-sensed reflectance
                at this pixel.
            parameters (sambuca.FreeParameters): If the pixel converged,
                the final parameters; otherwise None.
        """
        pass
