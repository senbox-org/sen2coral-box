# -*- coding: utf-8 -*-
"""
Created on Mon Feb  6 14:54:34 2017

@author: Marco
"""
import sys
sys.path.append('C:\\Progetti\\sambuca_project\\called_modules\\')
import input_sensor_filter
import input_parameters
import input_prepare
import output_calculation




class main_sambuca:
    def __init__(self):
        pass

    def main_sambuca_func(self,observed_rrs,observed_rrs_width, observed_rrs_height):
        self.observed_rrs=observed_rrs
        self.observed_rrs_width=observed_rrs_width
        self.observed_rrs_height=observed_rrs_height
        [self.sensor_filter, self.nedr]=input_sensor_filter.input_sensor_filter()
        #self.p0_rand, self.p0_mid, self.num_params,self.pmin, self.pmax, self.p_bounds, self.awater,  self.aphy_star, self.substrates,  self.substrate_names]=input_parameters.sam_par(parameters_path,substrates_path)
        [xstart, xend, ystart, yend,self.p0_rand, self.p0_mid, self.num_params,self.pmin, self.pmax, self.p_bounds, self.awater,  self.aphy_star, self.substrates,  self.substrate_names]=input_parameters.sam_par()
        [self.wavelengths, self.awater, self.aphy_star, self.substrates, self.sensor_filter, self.fixed_parameters, self.result_recorder, self.objective]=input_prepare.input_prepare(self.awater, self.aphy_star, self.substrates,  self.substrate_names, self.sensor_filter, self.observed_rrs_width, self.observed_rrs_height, self.nedr)
        
        
        
        if __name__=='main_sambuca_snap':  
            
            

    #pool=None
            result_recorder=output_calculation.output_calculation(xstart, xend, ystart, yend, observed_rrs, self.objective, self.p0_mid, self.p_bounds, self.result_recorder)
            return result_recorder.depth