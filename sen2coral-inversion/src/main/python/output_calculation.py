# -*- coding: utf-8 -*-
"""
Created on Mon Feb  6 19:08:35 2017

@author: Marco
"""

import sambuca as sb

import time
import multiprocessing as mp
import numpy as np




def output_calculation(xstart, xend, ystart, yend, observed_rrs, objective, p0_mid, p_bounds, result_recorder):
    pool=None
    #pool = mp.Pool(processes=10)
    n = 0
    skip_count = 0
    p0 = p0_mid
    t0 = time.time()
    
    for x in range(xstart, xend):
        for y in range(ystart, yend):
            print ([x,y])
            obs_rrs = observed_rrs[:,x,y]
            
            # Quick and dirty check because we are not masking out the no-data pixels
            if not np.allclose(obs_rrs, 0):
                
                # we need to set the observed rrs for this pixel into the objective, as there is no
                # direct way to get the scipy.minimise function to do it (although there are other ways
                # such as using a closure)
    
                #print("sono qui")

                result = sb.minimize(
                            objective,
                            p0,
                            method='SLSQP',
                            bounds=p_bounds,
                            options={'disp':False, 'maxiter':50},
                            obs_rrs=obs_rrs,
                            pool=pool)

                   
                #%time result = minimize(objective, p0, method='SLSQP', bounds=p_bounds, options={'disp':False, 'maxiter':500})
    
                # todo: check if the minimiser converged!
                
                # we need to repack the parameter tuple used by scipy.minimize into the sambuca.FreeParameter tuple
                # expected by the pixel result handlers. As the p0 tuple was generated from a FreeParameter tuple in the 
                # first place, we know that the order of the values match, so we can simply unpack the result tuple into 
                # the FreeParameters constructor.
                #print(result.nit,result.success,*result['x'])
                result_recorder(x, y, obs_rrs, parameters=sb.FreeParameters(*result.x), id=result.id, nit=result.nit, success=result.success)
                #result_recorder(x, y, obs_rrs, parameters=sb.FreeParameters(*result['x']))
            else:
                skip_count += 1
                #skip_count_widget.value = 'Pixels skipped (bad input spectra): {0}'.format(skip_count)
            
            # update the progress bar
            n += 1
            #text_widget.value = 'x: {0}  y: {1}  n: {2}'.format(x, y, n)
            #percentage_widget.value = 'Percentage complete: {0}%'.format(int(100*n/(num_pixels)))
            #progress_bar.value = n
    if pool != None:
        pool.close()
    
    t1 = time.time()
    print("Total execution time: {0:.1f} seconds".format(t1-t0))
    print("Average time per pixel: {0:.3f} seconds".format((t1-t0)/n))
    return result_recorder