# -*- coding: utf-8 -*-
"""
Created on Mon Feb  6 16:15:29 2017

@author: Marco
"""

from collections import namedtuple
from pkg_resources import resource_filename
from os.path import join
import os.path
import numpy as np
import numpy.ma as ma
import matplotlib.pyplot as plt
#import rasterio
from itertools import combinations
import time
import multiprocessing as mp
import sambuca as sb
import sambuca_core as sbc
#from sambuca_obs import sam_obs
#from sambuca_par import sam_par




def sam_par():
    if __name__=='input_parameters':
        
        base_path = 'D:\\Users\\obarrile\\Documents\\sen2coral\\Omar_Sambuca\\bioopti_data'
        
        substrate_path='D:\\Users\\obarrile\\Documents\\sen2coral\\Omar_Sambuca\\bioopti_data\\Substrates'
        substrate1_name = 'moreton_bay_speclib:white Sand'
        substrate2_name = 'moreton_bay_speclib:brown Mud'
        substrate3_name = 'moreton_bay_speclib:Syringodium isoetifolium'
        substrate4_name = 'moreton_bay_speclib:brown algae'
        substrate5_name = 'moreton_bay_speclib:green algae'
        #substrate_names= ( substrate1_name, substrate2_name)
        #substrate_names= ( substrate1_name, substrate2_name, substrate3_name)
        #substrate_names= ( substrate1_name, substrate2_name, substrate3_name, substrate4_name)
        substrate_names= ( substrate1_name, substrate2_name, substrate3_name, substrate4_name, substrate5_name)
        
        aphy_star_path = join(base_path, 'SIOP/WL08_aphy_1nm.hdr')
        aphy_star_name = 'wl08_aphy_1nm:WL08_aphy_star_mean_correct.csv:C2'
        
        awater_path = join(base_path, 'SIOP/aw_350_900_lw2002_1nm.csv')
        awater_name = 'aw_350_900_lw2002_1nm:a_water'
        
        all_substrates = sbc.load_all_spectral_libraries(substrate_path)
        substrates = []
        for substrate_name in substrate_names:
            substrates.append(all_substrates[substrate_name])
        # load all filters from the given directory

        
        aphy_star = sbc.load_spectral_library(aphy_star_path)[aphy_star_name]
        awater = sbc.load_spectral_library(awater_path)[awater_name]
        
        
        p_min = sb.FreeParameters(
            chl=0.01,               # Concentration of chlorophyll (algal organic particulates)
            cdom=0.0005,            # Concentration of coloured dissolved organic particulates
            nap=0.2,                # Concentration of non-algal particulates
            depth=0.1,              # Water column depth
            substrate_fraction=0)   # relative proportion of substrate1 and substrate2
        
        
        #p_max = sen.FreeParameters(
        #    chl=0.22, 
        #    cdom=0.015, 
        #    nap=2.4,
        #    depth=17.4,
        #    substrate_fraction=1)
        p_max = sb.FreeParameters(
            chl=0.16,
            cdom=0.01,
            nap=1.5,
            depth=7,
            substrate_fraction=1)
        
        #Create some initial parameters, one random and one as the mid point of each parameter range:
             
        pmin = np.array(p_min)

        pmax = np.array(p_max)

        num_params = len(pmin)
        p0_rand = np.random.random(num_params) * (pmax - pmin) + pmin
        p0_mid = (pmax - pmin) / 2
        
        print('p0_rand: ', p0_rand)
        print('p0_mid: ', p0_mid)
        
        
        # repackage p_min and p_max into the tuple of (min,max) pairs expected by our objective function,
        # and by the minimisation methods that support bounds
        p_bounds = tuple(zip(p_min, p_max))
        print('p_bounds', p_bounds)
        
        xstart = 0
        xend = 53
        xspan = xend - xstart
        ystart = 0
        yend = 21
        num_pixels = xspan * (yend - ystart)
        
        return xstart, xend, ystart, yend, p0_rand, p0_mid, num_params,pmin, pmax, p_bounds, awater,  aphy_star, substrates,  substrate_names