# -*- coding: utf-8 -*-
""" Sambuca exception definitions. """

from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)
from builtins import *


class SambucaException(Exception):
    """Root exception class for Sambuca exceptions.

    Only used as a base class for any Sambuca errors.
    This exception is never raised directly.
    """
    pass


class UnsupportedDataFormatError(SambucaException):
    """The file format is not supported by Sambuca."""
    pass


class DataValidationError(SambucaException):
    """The data file failed validation."""
    pass
