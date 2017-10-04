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
#import rasterio
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
        #TODO check length of nedrw and nedrs
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
        #TODO check length of sf_dict (it should be nw)
        #TODO check length of sf_dict[i] (it should be equal to len(sfw))
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
        #print ('EXIT SENSOR FILTER')
    
        return observed_rrs, image_info

def read_sensor_filter(sensor_xml_path):
        if __name__=='input_sensor_filter':
            xml=open(sensor_xml_path, 'rb')
            my_dict=xmltodict.parse(xml.read())
            nedrw=my_dict['root']['nedr']['item'][0]['item']
            nedrs=my_dict['root']['nedr']['item'][1]['item']
            #we map strings into float
            #TODO check length of nedrw and nedrs
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
            #TODO check length of sf_dict (it should be nw)
            #TODO check length of sf_dict[i] (it should be equal to len(sfw))
            for i in range(nw):
                sf.append(np.array(list(map(float,sf_dict[i]['item']))))
            sfs=np.array(sf) #the array with the filters spectra

            sfwm=np.array(list(map(float, sfw)))
            sfs=np.array(sf)
            #we create the tuple for the sensor filter
            sensor_filter=tuple([sfwm, sfs])

            #print ('EXIT SENSOR FILTER')

            return sensor_filter, nedr

def read_sensor_filter(sensor_xml_path,processingBands):
    if __name__=='input_sensor_filter':

        filter_bands = []
        for i in range(len(processingBands)):
            filter_bands.append(processingBands[i] != 'NULL' and processingBands[i] != 'null')
        xml=open(sensor_xml_path, 'rb')
        my_dict=xmltodict.parse(xml.read())
        nedrw=my_dict['root']['nedr']['item'][0]['item']
        nedrs=my_dict['root']['nedr']['item'][1]['item']
        #we map strings into float
        #TODO check length of nedrw and nedrs
        nedrw_m=np.array(list(map(float, nedrw)))
        nedrs_m=np.array(list(map(float, nedrs)))

        #nw_original is the nunmber of central wavelenghts
        nw_original=len(nedrw)

        #filter
        nedrw_m = nedrw_m[filter_bands]
        nedrs_m = nedrs_m[filter_bands]
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
        #TODO check length of sf_dict (it should be nw)
        #TODO check length of sf_dict[i] (it should be equal to len(sfw))
        for i in range(nw_original):
            sf.append(np.array(list(map(float,sf_dict[i]['item']))))
        sfs=np.array(sf) #the array with the filters spectra
        #filter bands
        sfs=sfs[filter_bands]

        sfwm=np.array(list(map(float, sfw)))
        #we create the tuple for the sensor filter
        sensor_filter=tuple([sfwm, sfs])

        return sensor_filter, nedr