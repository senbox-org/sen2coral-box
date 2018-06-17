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
import numpy as np

#from .error import distance_f


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
            error_function_name='alpha_f',
            nedr=None):
        """
        Initialise the ArrayWriter.
        Args:
            sensor_filter (array-like): The Sambuca sensor filter.
            fixed_parameters (sambuca.AllParameters): The fixed model
                parameters.
            error_function_name (string): The error function that will be applied
                to the modelled and observed rrs.
            nedr (array-like): Noise equivalent difference in reflectance.
        """
        super().__init__()

        # check for being passed the (wavelengths, filter) tuple loaded by the
        # sambuca_core sensor_filter loading functions
        if isinstance(sensor_filter, tuple) and len(sensor_filter) == 2:
            self._sensor_filter = sensor_filter[1]
            self._nedr = nedr[1]
        else:
            self._sensor_filter = sensor_filter
            self._nedr = nedr

        #self._nedr = nedr
        self._fixed_parameters = fixed_parameters
        self._error_func_name = error_function_name
        self.observed_rrs = None
        #self.id = None

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
        #id1 = self._fixed_parameters.substrate_combinations[self.id][0]
        #id2 = self._fixed_parameters.substrate_combinations[self.id][1]

        # Generate results from the given parameters
        model_results = sbc.forward_model(
            chl=parameters[0],
            cdom=parameters[1],
            nap=parameters[2],
            depth=parameters[3],
            sub1_frac=parameters[4],
            sub2_frac=parameters[5],
            sub3_frac=parameters[6],
            substrate1=self._fixed_parameters.substrates[0],
            substrate2=self._fixed_parameters.substrates[1],
            substrate3=self._fixed_parameters.substrates[2],
            wavelengths=self._fixed_parameters.wavelengths,
            a_water=self._fixed_parameters.a_water,
            a_ph_star=self._fixed_parameters.a_ph_star,
            num_bands=self._fixed_parameters.num_bands,
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
        closed_rrs_dchl = sbc.apply_sensor_filter(
            model_results.rrs_dchl,
            self._sensor_filter)
        closed_rrs_dcdom = sbc.apply_sensor_filter(
            model_results.rrs_dcdom,
            self._sensor_filter)
        closed_rrs_dnap = sbc.apply_sensor_filter(
            model_results.rrs_dnap,
            self._sensor_filter)
        closed_rrs_ddepth = sbc.apply_sensor_filter(
            model_results.rrs_ddepth,
            self._sensor_filter)			
        closed_rrs_dfrac1 = sbc.apply_sensor_filter(
            model_results.rrs_dfrac1,
            self._sensor_filter)			
        closed_rrs_dfrac2 = sbc.apply_sensor_filter(
            model_results.rrs_dfrac2,
            self._sensor_filter)			
        closed_rrs_dfrac3 = sbc.apply_sensor_filter(
            model_results.rrs_dfrac3,
            self._sensor_filter)

        print('AA')
        if self._error_func_name == 'lsq':
            C = np.linalg.norm(closed_rrs-self.observed_rrs)
            C_dcdom = np.sum((self.observed_rrs-closed_rrs)*(-closed_rrs_dcdom))/C
            C_dchl = np.sum((self.observed_rrs-closed_rrs)*(-closed_rrs_dchl))/C
            C_dnap = np.sum((self.observed_rrs-closed_rrs)*(-closed_rrs_dnap))/C
            C_ddepth = np.sum((self.observed_rrs-closed_rrs)*(-closed_rrs_ddepth))/C
            C_dfrac1 = np.sum((self.observed_rrs-closed_rrs)*(-closed_rrs_dfrac1))/C
            C_dfrac2 = np.sum((self.observed_rrs-closed_rrs)*(-closed_rrs_dfrac2))/C
            C_dfrac3 = np.sum((self.observed_rrs-closed_rrs)*(-closed_rrs_dfrac3))/C
            jacobian = np.array([C_dchl,C_dcdom,C_dnap,C_ddepth,C_dfrac1,C_dfrac2,C_dfrac3])
            return C,jacobian

        if self._nedr is not None:
        # deliberately avoiding an in-place divide as I want a copy of the
        # spectra to avoid side-effects due to pass by reference semantics
            print('AA2')
            print(self._nedr.shape)
            print(self._sensor_filter.shape)
            print(closed_rrs)
            closed_rrs_dchl = closed_rrs_dchl / self._nedr
            closed_rrs_dcdom = closed_rrs_dcdom / self._nedr
            closed_rrs_dnap = closed_rrs_dnap / self._nedr
            closed_rrs_ddepth = closed_rrs_ddepth / self._nedr
            closed_rrs_dfrac1 = closed_rrs_dfrac1 / self._nedr
            closed_rrs_dfrac2 = closed_rrs_dfrac2 / self._nedr
            closed_rrs_dfrac3 = closed_rrs_dfrac3 / self._nedr
            closed_rrs = closed_rrs / self._nedr
            observed_rrsNedr = self.observed_rrs / self._nedr
            print('AA3')
        #alpha_f=A*B
        normClosed_rss = np.linalg.norm(closed_rrs)
        normObserved_rss = np.linalg.norm(observed_rrsNedr)
        F = normClosed_rss*normObserved_rss
        F = np.clip(F, 1e-9, F)
        F2 = F*F
        #F2 = np.clip(F2, 1e-9, F2)
        E = np.sum(closed_rrs * observed_rrsNedr)
        G = E/F
        G = np.clip(G, 0.0, 1.0)
        G_div = np.power((1.0-G*G),0.5)
        #G_div = np.clip(G_div, 1e-9, G_div)
        D = np.sum(observed_rrsNedr)
        C = np.linalg.norm(closed_rrs-observed_rrsNedr)
        #C = np.clip(C, 1e-9, C)
        B = np.arccos(G)
        A = C/D
        print('AA4')
        F_dcdom = normObserved_rss * np.sum(closed_rrs * closed_rrs_dcdom)/normClosed_rss
        E_dcdom = np.sum(observed_rrsNedr*closed_rrs_dcdom)
        G_dcdom = (E_dcdom*F - F_dcdom*E)/F2
        C_dcdom = np.sum((observed_rrsNedr-closed_rrs)*(-closed_rrs_dcdom))/C
        B_dcdom = -G_dcdom/G_div
        A_dcdom = C_dcdom/D
        
        
        F_dchl = normObserved_rss * np.sum(closed_rrs * closed_rrs_dchl)/normClosed_rss
        E_dchl = np.sum(observed_rrsNedr*closed_rrs_dchl)
        G_dchl = (E_dchl*F - F_dchl*E)/F2
        C_dchl = np.sum((observed_rrsNedr-closed_rrs)*(-closed_rrs_dchl))/C
        B_dchl = -G_dchl/G_div
        A_dchl = C_dchl/D
        
        
        F_dnap = normObserved_rss * np.sum(closed_rrs * closed_rrs_dnap)/normClosed_rss
        E_dnap = np.sum(observed_rrsNedr*closed_rrs_dnap)
        G_dnap = (E_dnap*F - F_dnap*E)/F2
        C_dnap = np.sum((observed_rrsNedr-closed_rrs)*(-closed_rrs_dnap))/C
        B_dnap = -G_dnap/G_div
        A_dnap = C_dnap/D
        
        
        F_ddepth = normObserved_rss * np.sum(closed_rrs * closed_rrs_ddepth)/normClosed_rss
        E_ddepth = np.sum(observed_rrsNedr*closed_rrs_ddepth)
        G_ddepth = (E_ddepth*F - F_ddepth*E)/F2
        C_ddepth = np.sum((observed_rrsNedr-closed_rrs)*(-closed_rrs_ddepth))/C
        B_ddepth = -G_ddepth/G_div
        A_ddepth = C_ddepth/D
        
        
        F_dfrac1 = normObserved_rss * np.sum(closed_rrs * closed_rrs_dfrac1)/normClosed_rss
        E_dfrac1 = np.sum(observed_rrsNedr*closed_rrs_dfrac1)
        G_dfrac1 = (E_dfrac1*F - F_dfrac1*E)/F2
        C_dfrac1 = np.sum((observed_rrsNedr-closed_rrs)*(-closed_rrs_dfrac1))/C
        B_dfrac1 = -G_dfrac1/G_div
        A_dfrac1 = C_dfrac1/D
        
        
        F_dfrac2 = normObserved_rss * np.sum(closed_rrs * closed_rrs_dfrac2)/normClosed_rss
        E_dfrac2 = np.sum(observed_rrsNedr*closed_rrs_dfrac2)
        G_dfrac2 = (E_dfrac2*F - F_dfrac2*E)/F2
        C_dfrac2 = np.sum((observed_rrsNedr-closed_rrs)*(-closed_rrs_dfrac2))/C
        B_dfrac2 = -G_dfrac2/G_div
        A_dfrac2 = C_dfrac2/D
        
        
        F_dfrac3 = normObserved_rss * np.sum(closed_rrs * closed_rrs_dfrac3)/normClosed_rss
        E_dfrac3 = np.sum(observed_rrsNedr*closed_rrs_dfrac3)
        G_dfrac3 = (E_dfrac3*F - F_dfrac3*E)/F2
        C_dfrac3 = np.sum((observed_rrsNedr-closed_rrs)*(-closed_rrs_dfrac3))/C
        B_dfrac3 = -G_dfrac3/G_div
        A_dfrac3 = C_dfrac3/D
        
 
        if self._error_func_name == 'alpha_f':
            Jac_cdom = B * A_dcdom + B_dcdom*A
            Jac_chl = B * A_dchl + B_dchl*A
            Jac_nap = B * A_dnap + B_dnap*A
            Jac_depth = B * A_ddepth + B_ddepth*A
            Jac_frac1 = B * A_dfrac1 + B_dfrac1*A
            Jac_frac2 = B * A_dfrac2 + B_dfrac2*A
            Jac_frac3 = B * A_dfrac3 + B_dfrac3*A
            jacobian = np.array([Jac_chl,Jac_cdom,Jac_nap,Jac_depth,Jac_frac1,Jac_frac2,Jac_frac3])
            errorValue =  A*B

        if self._error_func_name == 'alpha':
            jacobian = np.array([B_dchl,B_dcdom,B_dnap,B_ddepth,B_dfrac1,B_dfrac2,B_dfrac3])
            errorValue =  B
        
        if self._error_func_name == 'f':
            jacobian = np.array([A_dchl,A_dcdom,A_dnap,A_ddepth,A_dfrac1,A_dfrac2,A_dfrac3])
            errorValue =  A

        
        #return self._error_func(self.observed_rrs, closed_rrs, self._nedr),jacobian
        #print("A*B{0}  A*B{1:.6f} A*B{2:.6f} A*B{3:.6f} A*B{4:.6f} A*B{5:.6f} A*B{6:.6f} A*B{7:.6f}".format(A*B,jacobian[0],jacobian[1],jacobian[2],jacobian[3],jacobian[4],jacobian[5],jacobian[6]))
        #print("Z{0}  {1:.6f} {2:.6f} {3:.6f} {4:.6f} {5:.6f} {6:.6f}".format(parameters[0],parameters[1],parameters[2],parameters[3],parameters[4],parameters[5],parameters[6]))
        #print("A{0}  B{1:.6f} C{2:.6f} D{3:.6f} E{4:.6f} F{5:.6f} G{6:.6f}".format(A,B,C,D,E,F,G))
        return errorValue,jacobian
