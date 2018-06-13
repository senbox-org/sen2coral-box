""" Pixel result handler that writes model outputs to numpy arrays.
"""


from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)
from builtins import *

import numpy as np
import sambuca_core as sbc

from .error import error_all
from .pixel_result_handler import PixelResultHandler


class ArrayResultWriter(PixelResultHandler):
    """ Pixel result handler that writes pixel model outputs
    to in-memory numpy arrays.

    **Note that the current implementation is writing a hard-coded set of outputs
    for the alpha implementation. The intention is to replace this with a
    data-driven system that only captures the outputs specified by the user.**

    **In the short-term, this class could be modified to add additional outputs,
    but this is not an ideal long-term solution.**

    The intent is that this class is used to capture model outputs of interest
    during raster processing. Once processing is complete, these outputs can
    then be used in various ways, such as writing to file (HDF, NetCDF,
    multi-band raster etc), plotting, or communication via message passing to a
    parallel processing manager. Each of these uses can be built on top of the
    basic array capture implemented in this class.

    """

    def __init__(
            self,
            width,
            height,
            sensor_filter,
            nedr,
            fixed_parameters):
        """
        Initialise the ArrayWriter.
        Args:
            width (int): Width in pixels of the modelled region.
            height (int): Height in pixels of the modelled region.
            sensor_filter (array-like): The Sambuca sensor filter.
            nedr (array-like): Noise equivalent difference in reflectance.
            fixed_parameters (sambuca.AllParameters): The fixed model
                parameters.
        """
        super().__init__()

        self._width = width
        self._height = height
        self._nedr = nedr
        self._fixed_parameters = fixed_parameters

        # check for being passed the (wavelengths, filter) tuple loaded by the
        # sambuca_core sensor_filter loading functions
        if isinstance(sensor_filter, tuple) and len(sensor_filter) == 2:
            self._sensor_filter = sensor_filter[1]
        else:
            self._sensor_filter = sensor_filter

        self._num_modelled_bands = self._sensor_filter.shape[1]
        self._num_observed_bands = self._sensor_filter.shape[0]

        # initialise the ndarrays for the outputs.
        # Note that I am hard-coding these outputs for now, but the intent is that this class
        # support a customisable list of outputs.
        self.error_alpha = np.zeros((width, height))
        self.error_alpha_f = np.zeros((width, height))
        self.error_f = np.zeros((width, height))
        self.error_lsq = np.zeros((width, height))
        self.chl = np.zeros((width, height))
        self.cdom = np.zeros((width, height))
        self.nap = np.zeros((width, height))
        self.depth = np.zeros((width, height))
        self.substrate_fraction = np.zeros((width, height))
        self.closed_rrs = np.zeros((width, height, self._num_observed_bands))
        self.nit = np.full((width, height), -1, dtype=np.int64)
        self.success = np.full((width, height), -1, dtype=np.int64)
        self.substrate_pair = np.full((width, height), -1, dtype=np.int64)

    def __call__(self, x, y, observed_rrs, parameters=None, id=None, nit=None, success=None):
        """
        Called by the parameter estimator when there is a result for a pixel.

        Args:
            x (int): The pixel x coordinate.
            y (int): The pixel y coordinate.
            observed_rrs (array-like): The observed remotely-sensed reflectance
                at this pixel.
            parameters (sambuca.FreeParameters): If the pixel converged,
                this contains the final parameters; otherwise None.
            id (int): The substrate combination index
            nit (int): The number of iterations performed
            success (bool): If the optimizer exited successfully
        """

        super().__call__(x, y, observed_rrs, parameters)

        # If this pixel did not converge, then there is nothing more to do
        if not parameters:
            return

        # Select the substrate pair from the list of substrates
        id1 = self._fixed_parameters.substrate_combinations[id][0]
        id2 = self._fixed_parameters.substrate_combinations[id][1]

        # Generate results from the given parameters
        model_results = sbc.forward_model(
            parameters.chl,
            parameters.cdom,
            parameters.nap,
            parameters.depth,
            self._fixed_parameters.substrates[id1],
            self._fixed_parameters.wavelengths,
            self._fixed_parameters.a_water,
            self._fixed_parameters.a_ph_star,
            self._fixed_parameters.num_bands,
            substrate_fraction=parameters.substrate_fraction,
            substrate2=self._fixed_parameters.substrates[id2],
            a_cdom_slope=self._fixed_parameters.a_cdom_slope,
            a_nap_slope=self._fixed_parameters.a_nap_slope,
            bb_ph_slope=self._fixed_parameters.bb_ph_slope,
            bb_nap_slope=self._fixed_parameters.bb_nap_slope,
            lambda0cdom=self._fixed_parameters.lambda0cdom,
            lambda0nap=self._fixed_parameters.lambda0nap,
            lambda0x=self._fixed_parameters.lambda0x,
            x_ph_lambda0x=self._fixed_parameters.x_ph_lambda0x,
            x_nap_lambda0x=self._fixed_parameters.x_nap_lambda0x,
            a_cdom_lambda0cdom=self._fixed_parameters.a_cdom_lambda0cdom,
            a_nap_lambda0nap=self._fixed_parameters.a_nap_lambda0nap,
            bb_lambda_ref=self._fixed_parameters.bb_lambda_ref,
            water_refractive_index=self._fixed_parameters.water_refractive_index,
            theta_air=self._fixed_parameters.theta_air,
            off_nadir=self._fixed_parameters.off_nadir,
            q_factor=self._fixed_parameters.q_factor)

        closed_rrs = sbc.apply_sensor_filter(
            model_results.rrs,
            self._sensor_filter)

        error = error_all(observed_rrs, closed_rrs, self._nedr)

        # Write the results into our arrays
        self.error_alpha[x,y] = error.alpha
        self.error_alpha_f[x,y] = error.alpha_f
        self.error_f[x,y] = error.f
        self.error_lsq[x,y] = error.lsq
        self.chl[x,y] = parameters.chl
        self.cdom[x,y] = parameters.cdom
        self.nap[x,y] = parameters.nap
        self.depth[x,y] = parameters.depth
        self.substrate_fraction[x,y] = parameters.substrate_fraction
        self.closed_rrs[x,y,:] = closed_rrs
        self.nit[x,y] = nit
        self.success[x,y] = success
        self.substrate_pair[x,y] = id
