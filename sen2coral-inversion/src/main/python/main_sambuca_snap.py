# -*- coding: utf-8 -*-
"""
Created on Mon Feb  6 14:54:34 2017

@author: Marco
"""

import input_sensor_filter
import input_parameters
import input_prepare
import output_calculation
import define_outputs




class main_sambuca:
    def __init__(self):
        pass

    def main_sambuca_func(self,observed_rrs,observed_rrs_width, observed_rrs_height, sensor_xml_path, siop_xml_path, par_xml_path, above_rrs_flag, shallow_flag, error_name, opt_met, relaxed):
        #self.observed_rrs=observed_rrs
        #self.observed_rrs_width=observed_rrs_width
        #self.observed_rrs_height=observed_rrs_height
        #print (observed_rrs.shape)
        image_info={}
        image_info['observed_rrs_width']=observed_rrs_width
        image_info['observed_rrs_height']=observed_rrs_height
        #image_info['base_path'] = 'C:\\Progetti\\sambuca_project\\input_data\\'

        [observed_rrs,image_info]=input_sensor_filter.input_sensor_filter(sensor_xml_path,observed_rrs,image_info, Rrs = above_rrs_flag)
        #self.p0_rand, self.p0_mid, self.num_params,self.pmin, self.pmax, self.p_bounds, self.awater,  self.aphy_star, self.substrates,  self.substrate_names]=input_parameters.sam_par(parameters_path,substrates_path)
        [siop, envmeta]=input_parameters.sam_par(siop_xml_path, par_xml_path)
        [wavelengths, siop, image_info, fixed_parameters, result_recorder, objective]=input_prepare.input_prepare(siop, envmeta,
                                                                                                           image_info, error_name)



        #if __name__=='main_sambuca_snap':



    #pool=None
        result_recorder=output_calculation.output_calculation(observed_rrs, objective, siop,
                                                                        result_recorder, image_info, opt_met, relaxed, shallow = shallow_flag)
        [chl, cdom, nap, depth, nit, kd, sdi, sub1_frac, sub2_frac, sub3_frac, \
         error_f, total_abun, sub1_norm, sub2_norm, sub3_norm, rgbimg, r_sub]=define_outputs.output_suite(result_recorder, image_info)
        return depth, sdi, kd, error_f, r_sub, sub1_frac, sub2_frac, sub3_frac, nit