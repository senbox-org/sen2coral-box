""" Sambuca Minimize Wrapper

    This is a wrapper for the SciPy minimize function that can be used
    with a pool of workers.
"""


from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)
from builtins import *

import sys
from collections import namedtuple
from scipy.optimize import minimize as scipy_minimize


minimize_result = namedtuple('minimize_result','''x,nit,success,id''')
""" namedtuple containing return result of sb_minimize.

Attributes:
        x (ndarray): The solution of the optimization.
        nit (int): The total number of iterations.
        success (bool): Whether the optimizer succeeded or not
        id (int): The substrate pair id corresponding to the solution
"""

def pwork(id,objective,p0,method,bounds,options,obs_rrs):
    """
    This is a wrapper function for the SciPy minimize function as only
    top level functions can be pickled with the multiprocessing module

    Args:
	id (int): the substrate combination index to use
	objective (callable): the objective function
        p0 (ndarray): the initial guess
        method (str): the type of solver
	bounds (sequence): bounds for the variable solution
	options (dict): solver options
	obs_rrs (ndarray): initial observations
    """

    objective.id = id
    objective.observed_rrs = obs_rrs

    return scipy_minimize(
                    objective,
                    p0,
                    method=method,
                    bounds=bounds,
                    options=options)

def minimize(objective,p0,method,bounds,options,obs_rrs,pool=None):
    """
    This is a wrapper function that iterates over all the substrate combinations
    calling the SciPy minimize function for each combination.  It returns the
    result with the best fit.  It supports passing a pool of worker to
    parallelize over the substrate combinations.

    Args:
	objective (callable): the objective function
        p0 (ndarray): the initial guess
        method (str): the type of solver
	bounds (sequence): bounds for the variable solution
	options (dict): solver options
	obs_rrs (ndarray): initial observations
	pool (Pool, optional): a pool of processes for multiprocessing
    """
    
    Nc = len(objective._fixed_parameters.substrate_combinations)
    
    if pool == None:
        results = []
        for i in range(Nc):
            results.append(pwork(i,objective,p0,method,bounds,options,obs_rrs))
    else:
        results = [None]*Nc
        presults = [None]*Nc
    
        if __name__ == 'sambuca.minimize_wrapper':
            for i in range(Nc):
                presults[i] = pool.apply_async(pwork,args=(i,objective,p0,method,bounds,options,obs_rrs))

        for i, presult in enumerate(presults):
            results[i] = presult.get()
   
    min_fun = sys.maxsize
    id = -1
    tot_nit = 0
    for i, result in enumerate(results):
        if (result.fun < min_fun) & ( result.success ):
            min_fun = result.fun
            id = i
        tot_nit += result.nit
        
    return minimize_result(results[id].x, tot_nit, results[id].success, id)
