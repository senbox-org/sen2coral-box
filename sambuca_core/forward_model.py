# -*- coding: utf-8 -*-
"""Semi-analytical Lee/Sambuca forward model. """

from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)
from builtins import *
import math
from collections import namedtuple

import numpy as np

from .constants import REFRACTIVE_INDEX_SEAWATER

ForwardModelResults = namedtuple('ForwardModelResults',
                                 [
                                     'r_substratum',
                                     'rrs',
                                     'rrsdp',
                                     'r_0_minus',
                                     'rdp_0_minus',
                                     'kd',
                                     'kub',
                                     'kuc',
                                     'a',
                                     'a_ph_star',
                                     'a_cdom_star',
                                     'a_nap_star',
                                     'a_ph',
                                     'a_cdom',
                                     'a_nap',
                                     'a_water',
                                     'bb',
                                     'bb_ph_star',
                                     'bb_nap_star',
                                     'bb_ph',
                                     'bb_nap',
                                     'bb_water',
                                 ])
""" A namedtuple containing the forward model results.

Attributes:
    r_substratum (numpy.ndarray): The combined substrate, or substrate1 if the
        optional second substrate was not provided.
    rrs (numpy.ndarray): Modelled remotely-sensed reflectance.
    rrsdp (numpy.ndarray): Modelled optically-deep remotely-sensed reflectance.
    r_0_minus (numpy.ndarray): Modelled remotely-sensed closed reflectance
        (R(0-)).
    rdp_0_minus (numpy.ndarray): Modelled optically-deep remotely-sensed closed
        reflectance (Rdp(0-)).
    kd (numpy.ndarray): TODO
    kub (numpy.ndarray): TODO
    kuc (numpy.ndarray): TODO
    a (numpy.ndarray): Modelled total absorption (absorption due to water + a_ph
        + a_cdom + a_nap)
    a_ph_star (numpy.ndarray): Specific absorption of phytoplankton. Although
        this is an input to the Sambuca model, it is included here for ease of
        access by client code.
    a_cdom_star (numpy.ndarray): Modelled specific absorption of coloured
        dissolved organic particulates (CDOM).
    a_nap_star (numpy.ndarray): Modelled specific absorption of non-algal
        particulates (NAP).
    a_ph (numpy.ndarray): Modelled absorption of phytoplankton.
    a_cdom (numpy.ndarray): Modelled absorption of CDOM.
    a_nap (numpy.ndarray): Modelled absorption of NAP.
    a_water (numpy.ndarray): Absorption coefficient of water. Another model
        input included in the results structure for convenience.
    bb (numpy.ndarray): Modelled total backscatter (bb_water + bb_ph + bb_nap).
    bb_ph_star (numpy.ndarray): Modelled specific backscatter of phytoplankton.
    bb_nap_star (numpy.ndarray): Modelled specific backscatter of NAP.
    bb_ph (numpy.ndarray): Modelled backscatter of phytoplankton.
    bb_nap(numpy.ndarray): Modelled backscatter of NAP.
    bb_water(numpy.ndarray): Modelled backscatter of water.
"""


# pylint: disable=too-many-arguments
# pylint: disable=too-many-locals

# Disabling invalid-name as many of the common (and published) variable names
# in the Sambuca model are invalid according to Python conventions.
# pylint: disable=invalid-name
def forward_model(
        chl,
        cdom,
        nap,
        depth,
        sub1_frac,
        sub2_frac,
        sub3_frac,
        substrate1,
        substrate2,
        substrate3,
        wavelengths,
        a_water,
        a_ph_star,
        num_bands,
        a_cdom_slope=None,
        a_nap_slope=None,
        bb_ph_slope=None,
        bb_nap_slope=None,
        lambda0cdom=None,
        lambda0nap=None,
        lambda0x=None,
        x_ph_lambda0x=None,
        x_nap_lambda0x=None,
        a_cdom_lambda0cdom=None,
        a_nap_lambda0nap=None,
        bb_lambda_ref=None,
        water_refractive_index=None,
        theta_air=None,
        off_nadir=None,
        q_factor=None):
    """Semi-analytical Lee/Sambuca forward model.

    TODO: Extended description goes here.

    TODO: For those arguments which have units, the units should be stated.

    Args:
        chl (float): Concentration of chlorophyll (algal organic particulates).
        cdom (float): Concentration of coloured dissolved organic particulates
            (CDOM).
        nap (float): Concentration of non-algal particulates (NAP).
        depth (float): Water column depth.
        substrate1 (array-like): A benthic substrate.
        wavelengths (array-like): Central wavelengths of the modelled
            spectral bands.
        a_water (array-like): Absorption coefficient of pure water
        a_ph_star (array-like): Specific absorption of phytoplankton.
        num_bands (int): The number of spectral bands.
        substrate_fraction (float): Substrate proportion, used to generate a
            convex combination of substrate1 and substrate2.
        substrate2 (array-like, optional): A benthic substrate.
        a_cdom_slope (float, optional): slope of CDOM absorption
        a_nap_slope (float, optional): slope of NAP absorption
        bb_ph_slope (float, optional): Power law exponent for the
            phytoplankton backscattering coefficient.
        bb_nap_slope (float, optional): Power law exponent for the
            NAP backscattering coefficient. If no value is supplied, the default
            behaviour is to use the bb_ph_slope value.
        lambda0cdom (float, optional): Reference wavelength for CDOM absorption.
        lambda0nap (float, optional): Reference wavelength for NAP absorption.
        lambda0x (float, optional): Backscattering reference wavelength.
        x_ph_lambda0x (float, optional): Specific backscatter of chlorophyl
            at lambda0x.
        x_nap_lambda0x (float, optional): Specific backscatter of NAP
            at lambda0x.
        a_cdom_lambda0cdom (float, optional): Absorption of CDOM at lambda0cdom.
        a_nap_lambda0nap (float, optional): Absorption of NAP at lambda0nap.
        bb_lambda_ref (float, optional): Reference wavelength for backscattering
            coefficient.
        water_refractive_index (float, optional): refractive index of water.
        theta_air (float, optional): solar zenith angle in degrees.
        off_nadir (float, optional): off-nadir angle.
        q_factor (float, optional): q value for producing the R(0-) values from
            modelled remotely-sensed reflectance (rrs) values.

    Returns:
        ForwardModelResults: A namedtuple containing the model outputs.
    """

    assert len(substrate1) == num_bands
    if substrate2 is not None:
        assert len(substrate2) == num_bands
    if substrate3 is not None:
        assert len(substrate2) == num_bands
    assert len(wavelengths) == num_bands
    assert len(a_water) == num_bands
    assert len(a_ph_star) == num_bands

    # Sub-surface solar zenith angle in radians
    inv_refractive_index = 1.0 / water_refractive_index
    theta_w = \
        math.asin(inv_refractive_index * math.sin(math.radians(theta_air)))

    # Sub-surface viewing angle in radians
    theta_o = \
        math.asin(inv_refractive_index * math.sin(math.radians(off_nadir)))

    # Calculate derived SIOPS, based on
    # Mobley, Curtis D., 1994: Radiative Transfer in natural waters.
    bb_water = (0.00194 / 2.0) * np.power(bb_lambda_ref / wavelengths, 4.32)
    a_cdom_star = a_cdom_lambda0cdom * \
        np.exp(-a_cdom_slope * (wavelengths - lambda0cdom))
    a_nap_star = a_nap_lambda0nap * \
        np.exp(-a_nap_slope * (wavelengths - lambda0nap))

    # Calculate backscatter
    backscatter = np.power(lambda0x / wavelengths, bb_ph_slope)
    # specific backscatter due to phytoplankton
    bb_ph_star = x_ph_lambda0x * backscatter
    # specific backscatter due to NAP
     # If a bb_nap_slope value has been supplied, use it.
     # Otherwise, reuse bb_ph_slope.
    if bb_nap_slope:
        backscatter = np.power(lambda0x / wavelengths, bb_nap_slope)
    bb_nap_star = x_nap_lambda0x * backscatter

    # Total absorption
    a_ph = chl * a_ph_star
    a_cdom = cdom * a_cdom_star
    a_nap = nap * a_nap_star
    a = a_water + a_ph + a_cdom + a_nap

    # Total backscatter
    bb_ph = chl * bb_ph_star
    bb_nap = nap * bb_nap_star
    bb = bb_water + bb_ph + bb_nap

    # Calculate total bottom reflectance from the two substrates
    #r_substratum = substrate1
    #if substrate2 is not None:
        #r_substratum = substrate_fraction * substrate1 + \
        #(1. - substrate_fraction) * substrate2
        
    # Calculate bottom reflectance from 3 subs (BOMBER etc)
    
    r_substratum = sub1_frac * substrate1 + sub2_frac * substrate2 + sub3_frac * substrate3

    # TODO: what are u and kappa?
    kappa = a + bb
    u = bb / kappa

    # Optical path elongation for scattered photons
    # elongation from water column
    # TODO: reference to the paper from which these equations are derived
    du_column = 1.03 * np.power(1.00 + (2.40 * u), 0.50)
    # elongation from bottom
    du_bottom = 1.04 * np.power(1.00 + (5.40 * u), 0.50)

    # Remotely sensed sub-surface reflectance for optically deep water
    rrsdp = (0.084 + 0.17 * u) * u

    # common terms in the following calculations
    inv_cos_theta_w = 1.0 / math.cos(theta_w)
    inv_cos_theta_0 = 1.0 / math.cos(theta_o)
    du_column_scaled = du_column * inv_cos_theta_0
    du_bottom_scaled = du_bottom * inv_cos_theta_0

    # TODO: descriptions of kd, kuc, kub
    kd = kappa * inv_cos_theta_w
    kuc = kappa * du_column_scaled
    kub = kappa * du_bottom_scaled

    # Remotely sensed reflectance
    kappa_d = kappa * depth
    rrs = (rrsdp *
           (1.0 - np.exp(-(inv_cos_theta_w + du_column_scaled) * kappa_d)) +
           ((1.0 / math.pi) * r_substratum *
            np.exp(-(inv_cos_theta_w + du_bottom_scaled) * kappa_d)))

    return ForwardModelResults(
        r_substratum=r_substratum,
        rrs=rrs,
        rrsdp=rrsdp,
        r_0_minus=rrs * q_factor,
        rdp_0_minus=rrsdp * q_factor,
        kd=kd,
        kub=kub,
        kuc=kuc,
        a=a,
        a_ph_star=a_ph_star,
        a_cdom_star=a_cdom_star,
        a_nap_star=a_nap_star,
        a_ph=a_ph,
        a_cdom=a_cdom,
        a_nap=a_nap,
        a_water=a_water,
        bb=bb,
        bb_ph_star=bb_ph_star,
        bb_nap_star=bb_nap_star,
        bb_ph=bb_ph,
        bb_nap=bb_nap,
        bb_water=bb_water,
    )

# pylint: enable=too-many-arguments
# pylint: enable=invalid-name
# pylint: enable=too-many-locals
