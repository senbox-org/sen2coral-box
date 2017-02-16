# -*- coding: utf-8 -*-
"""
Created on Mon Feb  6 16:24:40 2017

@author: Marco
"""

import sambuca as sb
import sambuca_core as sbc
import numpy as np
from os.path import join
import os.path






def input_prepare(awater, aphy_star, substrates,  substrate_names, sensor_filter, observed_rrs_width, observed_rrs_height, nedr):

    wavelengths = sbc.spectra_find_common_wavelengths(awater, aphy_star, *substrates)
    print('Common wavelength range: {0} - {1}'.format(min(wavelengths), max(wavelengths)))
    
    #Use the common wavelengths to mask the inputs:
    awater = sbc.spectra_apply_wavelength_mask(awater, wavelengths)
    aphy_star = sbc.spectra_apply_wavelength_mask(aphy_star, wavelengths)
    for i, substrate in enumerate(substrates):
        substrates[i] = sbc.spectra_apply_wavelength_mask(substrate, wavelengths)
        
    print('awater: min: {0}  max: {1}'.format(min(awater[0]), max(awater[0])))
    print('aphy_star: min: {0}  max: {1}'.format(min(aphy_star[0]), max(aphy_star[0])))
    for substrate_name, substrate in zip(substrate_names, substrates):
        print('{0}: min: {1}  max: {2}'.format(substrate_name, min(substrate[0]), max(substrate[0])))
    
    
    """Truncate the sensor filter to match the common wavelength range
    It remains to be seen whether this is the best approach, but it works for this demo. An alternative approach would be to truncate the entire band for any band that falls outside the common wavelength range.
    If this approach, or something based on it, is valid, then this should be moved into a sambuca_core function with appropriate unit tests."""
    
    filter_mask = (sensor_filter[0] >= wavelengths.min()) & (sensor_filter[0] <= wavelengths.max())
    sensor_filter = sensor_filter[0][filter_mask], sensor_filter[1][:,filter_mask]
    
    
    fixed_parameters = sb.create_fixed_parameter_set(
                wavelengths=wavelengths,
                a_water=awater,
                a_ph_star=aphy_star,
                substrates=substrates,
                substrate_fraction=1,
                a_cdom_slope=0.0168052,
                a_nap_slope=0.00977262,
                bb_ph_slope=0.878138,
                bb_nap_slope=None,
                lambda0cdom=550.0,
                lambda0nap=550.0,
                lambda0x=546.0,
                x_ph_lambda0x=0.00157747,
                x_nap_lambda0x=0.0225353,
                a_cdom_lambda0cdom=1.0,
                a_nap_lambda0nap=0.00433,
                bb_lambda_ref=550,
                water_refractive_index=1.33784,
                theta_air=30.0,
                off_nadir=0.0,
                q_factor=np.pi,
                chl=0.01,
                cdom=0.0005,
                nap=0.2,
                depth=0.1)


    result_recorder = sb.ArrayResultWriter(
            observed_rrs_width,
            observed_rrs_height,
            sensor_filter,  
            nedr,
            fixed_parameters)
    objective = sb.SciPyObjective(sensor_filter, fixed_parameters, error_function=sb.distance_f, nedr=nedr)
    
    return wavelengths, awater, aphy_star, substrates, sensor_filter, fixed_parameters, result_recorder, objective