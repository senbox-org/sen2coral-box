# -*- coding: utf-8 -*-
"""
Created on Mon Feb  6 15:36:34 2017

@author: Marco
"""

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
import xmltodict

def input_sensor_filter(sensor_xml_path,observed_rrs,image_info, Rrs = False):
    if __name__=='input_sensor_filter':
        xml=open(sensor_xml_path, 'rb')
        my_dict=xmltodict.parse(xml.read())
        #we take the filename from the path chosen by the user
        #observed_rrs_filename=rrs_path.split('\\')[len(rrs_path.split('\\'))-1]
        nedrw=my_dict['root']['nedr']['item'][0]['item']
        nedrs=my_dict['root']['nedr']['item'][1]['item']
        #we map strings into float
        nedrw_m=np.array(list(map(float, nedrw)))
        nedrs_m=np.array(list(map(float, nedrs)))
        #we create the tuple for nedr
        nedr=tuple([nedrw_m, nedrs_m])
        #nw is the nunmber of central wavelenghts
        nw=len(nedrw)
        
        
        #in sfw we have the wlens of the sesnsor filters
        sfw=my_dict['root']['sensor_filter']['item'][0]['item']
        #in sf_dict we have the spectra of the filters
        sf_dict=my_dict['root']['sensor_filter']['item'][1]['item']
        sf=[] #intialize the list for the sensor spectra
        #we append to this list the spectra, mapping strings into float
        for i in range(nw):
            sf.append(np.array(list(map(float,sf_dict[i]['item']))))
        sfs=np.array(sf) #the array with the filters spectra
            
        sfwm=np.array(list(map(float, sfw)))
        sfs=np.array(sf)
        #we create the tuple for the sensor filter
        sensor_filter=tuple([sfwm, sfs])
        

        
        if Rrs == True:
            observed_rrs = (2*observed_rrs)/((3*observed_rrs)+1)
            
            
        image_info['sensor_filter']=sensor_filter
        image_info['nedr']=nedr
        print ('EXIT SENSOR FILTER')
    
        return observed_rrs, image_info