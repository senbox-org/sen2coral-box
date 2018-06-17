# -*- coding: utf-8 -*-
""" Core components of the Sambuca modeling system """

from .exceptions import (
    SambucaException,
    UnsupportedDataFormatError,
    DataValidationError
)
from .forward_model import forward_model, ForwardModelResults
from .sensor_filter import (
    apply_sensor_filter,
    load_sensor_filters,
    load_sensor_filters_excel,
    load_sensor_filter_spectral_library,
)
from .spectra_operations import (
    spectra_find_common_wavelengths,
    spectra_apply_wavelength_mask,
)
from .spectra_readers import (
    load_spectral_library,
    load_all_spectral_libraries,
    load_csv_spectral_library,
    load_envi_spectral_library,
    load_excel_spectral_library,
)
from .utility import (
    strictly_decreasing,
    strictly_increasing,
)

__author__ = 'Daniel Collins'
__email__ = 'daniel.collins@csiro.au'

# Versioning: major.minor.patch
# major: increment on a major version. Must be changed when the API changes in
# an imcompatible way.
# minor: new functionality that does not break the
# existing API.
# patch: bug-fixes that do not change the public API
__version__ = '1.3.3'
