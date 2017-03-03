# -*- coding: utf-8 -*-
"""
Created on Mon Feb  6 11:27:48 2017

@author: Marco
"""

"""import obs
import par
import com
import out
import prep"""
import snappy
import numpy as np
from snappy import jpy
Float = jpy.get_type('java.lang.Float')
Color = jpy.get_type('java.awt.Color')
FileIO= jpy.get_type ('java.io.File')
from snappy import jpy
import sambuca
import main_sambuca_snap
import os
#import snappy


class sambuca_snap_op:
    
    def __init__(self):
        self.algo = None
        self.depth_band = None
        self.sdi_band= None
        self.kd=None
        self.error_f=None
        self.r_sub=None
        self.sub1_frac=None
        self.sub2_frac=None
        self.sub3_frac=None

    def initialize(self, context):
        #read the source_product
        source_product = context.getSourceProduct('source')
        if source_product is None:
            source_product = context.getSourceProduct('sourceProduct')
        if source_product is None:
            return
        print('initialize: source product location is', source_product.getFileLocation())
        #get width and height of the image
        width = source_product.getSceneRasterWidth()
        height = source_product.getSceneRasterHeight()
        
        #get the names of the bands
        band_names = source_product.getBandNames()
        band_n=list(band_names)
        
        #choose the band with wavelenght between 420 and 750 nm
        self.min_w=context.getParameter('min_wlen')
        self.max_w=context.getParameter('max_wlen')
        band_l=[]
        y=[]
        for band_name in band_n:
            b=source_product.getBand(band_name)
            w=b.getSpectralWavelength()
            if w>self.min_w and w<self.max_w:
                band_l.append(b)
                y.append(w)
        #we order the band_list
        self.band_list=[band_l for (y,band_l) in sorted(zip(y,band_l))]
        
        
            
                
                
                
                
        #read the .xml file with nedr, sensor filters, parameters, SIOP and substrates
        self.sensor_xml_path=str(context.getParameter('xmlpath_sensor'))
        self.siop_xml_path=str(context.getParameter('xmlpath_siop'))
        self.par_xml_path=str(context.getParameter('xmlpath_parameters'))
        self.error_name=context.getParameter('error_name')
        self.opt_met=context.getParameter('opt_method')

        #read the flag for rrs and shallow (True or False)
        self.above_rrs_flag=context.getParameter('above_rrs_flag')
        self.shallow_flag=context.getParameter('shallow_flag')
        #define the sambuca algorithm
        self.algo=main_sambuca_snap.main_sambuca()
        #create the target product
        sambuca_product=snappy.Product('sambuca', 'sambuca', width, height)
        #import metadata and geocoding from the source_product
        snappy.ProductUtils.copyGeoCoding(source_product, sambuca_product)
        snappy.ProductUtils.copyMetadata(source_product, sambuca_product)   
        #create two ouput bands
        self.depth_band = sambuca_product.addBand('depth', snappy.ProductData.TYPE_FLOAT32)
        self.depth_band.setDescription('The depth computed by SAMBUCA')
        self.depth_band.setNoDataValue(Float.NaN)
        self.depth_band.setNoDataValueUsed(True)
        self.sdi_band = sambuca_product.addBand('sdi', snappy.ProductData.TYPE_FLOAT32)
        self.sdi_band.setDescription('The sdi computed by SAMBUCA')
        self.sdi_band.setNoDataValue(Float.NaN)
        self.sdi_band.setNoDataValueUsed(True)
        self.kd_band = sambuca_product.addBand('kd(550)', snappy.ProductData.TYPE_FLOAT32)
        self.kd_band.setDescription('The kd computed by SAMBUCA')
        self.kd_band.setNoDataValue(Float.NaN)
        self.kd_band.setNoDataValueUsed(True)
        self.error_f_band = sambuca_product.addBand('error_f', snappy.ProductData.TYPE_FLOAT32)
        self.error_f_band.setDescription('The error_f computed by SAMBUCA')
        self.error_f_band.setNoDataValue(Float.NaN)
        self.error_f_band.setNoDataValueUsed(True)
        self.r_sub_band = sambuca_product.addBand('r_sub(550)', snappy.ProductData.TYPE_FLOAT32)
        self.r_sub_band.setDescription('The r_sub(550) computed by SAMBUCA')
        self.r_sub_band.setNoDataValue(Float.NaN)
        self.r_sub_band.setNoDataValueUsed(True)
        self.sub1_frac_band = sambuca_product.addBand('sub_1', snappy.ProductData.TYPE_FLOAT32)
        self.sub1_frac_band.setDescription('The sub_1 % computed by SAMBUCA')
        self.sub1_frac_band.setNoDataValue(Float.NaN)
        self.sub1_frac_band.setNoDataValueUsed(True)
        self.sub2_frac_band = sambuca_product.addBand('sub_2', snappy.ProductData.TYPE_FLOAT32)
        self.sub2_frac_band.setDescription('The sub_2 % computed by SAMBUCA')
        self.sub2_frac_band.setNoDataValue(Float.NaN)
        self.sub2_frac_band.setNoDataValueUsed(True)
        self.sub3_frac_band = sambuca_product.addBand('sub_3', snappy.ProductData.TYPE_FLOAT32)
        self.sub3_frac_band.setDescription('The sub_3 % computed by SAMBUCA')
        self.sub3_frac_band.setNoDataValue(Float.NaN)
        self.sub3_frac_band.setNoDataValueUsed(True)

        
        context.setTargetProduct(sambuca_product)
        
    def computeTileStack(self, context, target_tiles, target_rectangle):
        
        #read the tiles
        tiles_list=[]
        for band in self.band_list:
            tiles_list.append(context.getSourceTile(band, target_rectangle))

        #read the rrs data of the tiles
        samples_list=[]
        for tile in tiles_list:
            samples_list.append(tile.getSamplesFloat())
        #create a list of samples
        rrs_list=[]
        for sample in samples_list:
            sample=np.resize(sample,(target_rectangle.height, target_rectangle.width) )
            sample=np.array(sample, dtype=np.float32)
            rrs_list.append(sample)
        #create the rrs matrix (wavelenghtsz x height x width)
        rrs=np.array(rrs_list)
        print (rrs.shape)
        


        #call the algorithm
        [depth, sdi, kd, error_f, r_sub, sub1_frac, sub2_frac, sub3_frac]=self.algo.main_sambuca_func(rrs, target_rectangle.width,\
                                          target_rectangle.height, self.sensor_xml_path,self.siop_xml_path, self.par_xml_path,\
                                          self.above_rrs_flag, self.shallow_flag, self.error_name, self.opt_met)
        #allocate the outputs in the output bands
        depth_tile=target_tiles.get(self.depth_band)
        depth_tile.setSamples(depth.flatten())
        
        sdi_tile=target_tiles.get(self.sdi_band)
        sdi_tile.setSamples(sdi.flatten())
        
        kd_tile=target_tiles.get(self.kd_band)
        kd_tile.setSamples(kd.flatten())
        
        error_f_tile=target_tiles.get(self.error_f_band)
        error_f_tile.setSamples(error_f.flatten())
        r_sub_tile=target_tiles.get(self.r_sub_band)
        r_sub_tile.setSamples(r_sub.flatten())
        sub1_frac_tile=target_tiles.get(self.sub1_frac_band)
        sub1_frac_tile.setSamples(sub1_frac.flatten())
        sub2_frac_tile=target_tiles.get(self.sub2_frac_band)
        sub2_frac_tile.setSamples(sub2_frac.flatten())
        sub3_frac_tile=target_tiles.get(self.sub3_frac_band)
        sub3_frac_tile.setSamples(sub3_frac.flatten())
        

    def doExecute(self, pm):
        pass

    def dispose(self, context):
        pass
        

    

    