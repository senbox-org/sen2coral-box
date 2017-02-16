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
from snappy import jpy
import sambuca
import main_sambuca_snap

class sambuca_snap_op:
    
    def __init__(self):
        self.rrs_0 = None
        self.rrs_1 = None
        self.rrs_2 = None
        self.rrs_3 = None
        self.algo = None
        self.depth_band = None

    def initialize(self, context):
        source_product = context.getSourceProduct('source')
        if source_product is None:
            source_product = context.getSourceProduct('sourceProduct')
        if source_product is None:
            return
        print('initialize: source product location is', source_product.getFileLocation())

        width = source_product.getSceneRasterWidth()
        height = source_product.getSceneRasterHeight()
        #from the .xml file we must choose all the rrs needed in the computation and we must define the name and self.rrs for all of them 
        rrs_0_name=context.getParameter('firstBand')  
        self.rrs_0= self._get_band(source_product, rrs_0_name)
        #from the .xml file we must choose all the rrs needed in the computation and we must define the name and self.rrs for all of them 
        rrs_1_name=context.getParameter('secondBand')  
        self.rrs_1= self._get_band(source_product, rrs_1_name)
        rrs_2_name=context.getParameter('thirdBand')  
        self.rrs_2= self._get_band(source_product, rrs_2_name)
        rrs_3_name=context.getParameter('fourthBand')  
        self.rrs_3= self._get_band(source_product, rrs_3_name)
        #from the GUI we must select the parameters thresholds
        #self.filters_path=context.getParameter('filters_path')#maybe is better provide the parameters by .txt file, because in the GUI can be difficult provide eight parameters 
        #self.parameters_path=context.getParameter('parameters_path')
        #self.substrates_path=context.getParameter('substrates_path')
                                              
    
        #substrate_path=context.getParameter('substrate_path')
        #filters_path=context.getParameter('filters_path')
        #parameters=[l_CHL,h_CHL] #we must add CDOM, depths and NAP. And the substrates composition? Maybe it's better give the parameters the same structure of sambuca (p0min,p0max..)
        #maybe we can add a parameter named sensor_name to choose the proper sensor_filters and substrates from the files
        #and if we want substrates and sensor_filters to be user definied? I think that we must choose the substrates path and the provide this is as input for the model 
        
        self.algo=main_sambuca_snap.main_sambuca()

        sambuca_product=snappy.Product('sambuca', 'sambuca', width, height)

        snappy.ProductUtils.copyGeoCoding(source_product, sambuca_product)
        snappy.ProductUtils.copyMetadata(source_product, sambuca_product)   
        #we have to add the different bands(depth, chl, cdom..); for the now only depth
        self.depth_band = sambuca_product.addBand('depth', snappy.ProductData.TYPE_FLOAT32)
        self.depth_band.setDescription('The depth computed by SAMBUCA')
        self.depth_band.setNoDataValue(Float.NaN)
        self.depth_band.setNoDataValueUsed(True)
        #for now I don't understand properly the flag coding
        
        context.setTargetProduct(sambuca_product)
        
    def computeTileStack(self, context, target_tiles, target_rectangle):
        tile_0 = context.getSourceTile(self.rrs_0, target_rectangle)
        tile_1= context.getSourceTile(self.rrs_1, target_rectangle)
        tile_2 = context.getSourceTile(self.rrs_2, target_rectangle)
        tile_3 = context.getSourceTile(self.rrs_3, target_rectangle)
        samples_0 = tile_0.getSamplesFloat()
        samples_1 = tile_1.getSamplesFloat()
        samples_2 = tile_2.getSamplesFloat()
        samples_3 = tile_3.getSamplesFloat()

        samples_0 = np.resize(samples_0,(target_rectangle.width, target_rectangle.height))
        samples_1 = np.resize(samples_1,(target_rectangle.width, target_rectangle.height))
        samples_2 = np.resize(samples_2,(target_rectangle.width, target_rectangle.height))
        samples_3 = np.resize(samples_3,(target_rectangle.width, target_rectangle.height))
        rrs=np.array([np.array(samples_0,dtype=np.float32), np.array(samples_1, dtype=np.float32), np.array(samples_2,dtype=np.float32), np.array(samples_3,dtype=np.float32)])



        depth=self.algo.main_sambuca_func(rrs, target_rectangle.width, target_rectangle.height)
        depth_tile=target_tiles.get(self.depth_band)
        #depth_tile.setSamples(depth)
        depth_tile.setSamples(depth.flatten())

    def doExecute(self, pm):
        pass

    def dispose(self, context):
        pass
        
    def _get_band(self, product, name):
        band = product.getBandGroup().get(name)
        if not band:
            raise RuntimeError('Product does not contain a band named', name)
        return band
        
    