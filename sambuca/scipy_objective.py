""" Objective function for parameter estimation using scipy minimisation.
"""


from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)
from builtins import *

from collections import Callable

import sambuca_core as sbc

from .error import distance_f


class SciPyObjective(Callable):
    """
    Configurable objective function for Sambuca parameter estimation, intended
    for use with the SciPy minimisation methods.

    Attributes:
        observed_rrs (array-like): The observed remotely-sensed reflectance.
            This attribute must be updated when you require the objective
            instance to use a different value.
        id (integer): The index of the substrate pair combination.
            This attribute must be updated when you require the objective
            instance to use a different substrate pair.
    """

    def __init__(
            self,
            sensor_filter,
            fixed_parameters,
            error_function=distance_f,
            nedr=None):
        """
        Initialise the ArrayWriter.
        Args:
            sensor_filter (array-like): The Sambuca sensor filter.
            fixed_parameters (sambuca.AllParameters): The fixed model
                parameters.
            error_function (Callable): The error function that will be applied
                to the modelled and observed rrs.
            nedr (array-like): Noise equivalent difference in reflectance.
        """
        super().__init__()

        # check for being passed the (wavelengths, filter) tuple loaded by the
        # sambuca_core sensor_filter loading functions
        if isinstance(sensor_filter, tuple) and len(sensor_filter) == 2:
            self._sensor_filter = sensor_filter[1]
        else:
            self._sensor_filter = sensor_filter

        self._nedr = nedr
        self._fixed_parameters = fixed_parameters
        self._error_func = error_function
        self.observed_rrs = None
        self.id = None

    def __call__(self, parameters):
        """
        Returns an objective score for the given parameter set.

        Args:
            parameters (ndarray): The parameter array in the order
                (chl, cdom, nap, depth, substrate_fraction)
                as defined in the FreeParameters tuple
          
        """

        # TODO: do I need to implement this? Here or in a subclass?
        # To support algorithms without support for boundary values, we assign a high
        # score to out of range parameters. This may not be the best approach!!!
        # p_bounds is a tuple of (min, max) pairs for each parameter in p
        '''
        if p_bounds is not None:
            for _p, lu in zip(p, p_bounds):
                l, u = lu
                if _p < l or _p > u:
                    return 100000.0
        '''

        # Select the substrate pair from the list of substrates
        id1 = self._fixed_parameters.substrate_combinations[self.id][0]
        id2 = self._fixed_parameters.substrate_combinations[self.id][1]

        # Generate results from the given parameters
        model_results = sbc.forward_model(
            chl=parameters[0],
            cdom=parameters[1],
            nap=parameters[2],
            depth=parameters[3],
            substrate_fraction=parameters[4],
            substrate1=self._fixed_parameters.substrates[id1],
            wavelengths=self._fixed_parameters.wavelengths,
            a_water=self._fixed_parameters.a_water,
            a_ph_star=self._fixed_parameters.a_ph_star,
            num_bands=self._fixed_parameters.num_bands,
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

        return self._error_func(self.observed_rrs, closed_rrs, self._nedr)
