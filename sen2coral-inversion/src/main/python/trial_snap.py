# -*- coding: utf-8 -*-
"""
Created on Thu Feb  9 12:54:39 2017

@author: Marco
"""
import sys

sys.path.append('C:\\Progetti\\sambuca_project\\sen2coral\\')
#print (sys.path)
#import par
#import obs
import sambuca_input_rrs
import sambuca_input_parameters
import sambuca_preparation
import sambuca_calculations
import sambuca_outputs
import main_sambuca_snap
"""Utility Functions"""

# The plot_items list is used to hold spectra for the plots
plot_items = []

def print_parameters(p):
    print(
'''\
    CHL:  {0:10.5f}
    CDOM: {1:10.5f}
    TR:   {2:10.5f}
    H:    {3:10.5f}
    Q:    {4:10.5f}'''
          .format(p.chl,p.cdom,p.nap,p.depth,p.substrate_fraction))
    



if __name__=='__main__':
    
    #[observed_rrs, observed_rrs_width, observed_rrs_height,  nedr, sensor_filter, xstart, xend, ystart, yend, num_pixels, base_path, observed_rrs_filename]=sambuca_input_rrs.sam_obs()
    [observed_rrs, image_info]=sambuca_input_rrs.sam_obs()
    algo=main_sambuca_snap.main_sambuca()
    algo.main_sambuca_func(observed_rrs, image_info['observed_rrs_width'], image_info['observed_rrs_height'])