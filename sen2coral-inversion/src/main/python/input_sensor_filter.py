# -*- coding: utf-8 -*-
"""
Created on Mon Feb  6 15:36:34 2017

@author: Marco
"""

from os.path import join
import os.path
import rasterio
import sambuca as sb
import sambuca_core as sbc
#import nibabel as nib
import snappy
import numpy as np

def input_sensor_filter():
    if __name__=='input_sensor_filter':
        
        
        
        base_path = 'D:\\Users\\obarrile\\Documents\\sen2coral\\Omar_Sambuca\\bioopti_data'
        sensor_filter_name = 'ALOS'
        sensor_filter_path = join(base_path, 'sensor_filters')
        
        observed_rrs_base_path='D:\\Users\\obarrile\\Documents\\sen2coral\\Omar_Sambuca\\bioopti_data\\wl_alos_data\\inputs'
        nedr_path = join(observed_rrs_base_path, 'WL_ALOS_NEDR_0_4bands.hdr')
        
        
        sensor_filter_name = 'ALOS'
        
        sensor_filters = sbc.load_sensor_filters(sensor_filter_path)
        # We don't need to do this, but it lets us see the name of all loaded filters
        sensor_filters.keys()
        
        
        # retrieve the specified filter
        sensor_filter = sensor_filters[sensor_filter_name]
        
        
        #Plot the sensor filter:
        #plot_items.clear()  #Python 3.3 and later only
        
        nedr = sbc.load_spectral_library(nedr_path, validate=False)['wl_alos_nedr_0_4bands:33']
        nedr
        xstart=0
        xend=5
        ystart=0
        yend=120
        return sensor_filter, nedr