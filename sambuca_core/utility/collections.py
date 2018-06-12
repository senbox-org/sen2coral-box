# -*- coding: utf-8 -*-
""" Operating system-specific utility functions. """

# Disable some pylint warnings caused by future and tkinter
# pylint: disable=unused-wildcard-import
# pylint: disable=redefined-builtin
# pylint: disable=wildcard-import
# pylint: disable=too-many-ancestors

# Ensure backwards compatibility with Python 2
from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals,
)
from builtins import *

from itertools import tee

def pairwise(iterable):
    """s -> (s0,s1), (s1,s2), (s2, s3), ..."""
    a, b = tee(iterable)
    next(b, None)
    return zip(a, b)

def merge_dictionary(target, new_items):
    """ Merges a dictionary into the master set,
    warning when a duplicate name is detected. Keys from new_items that
    are already present in target will generate warnings without modifying
    target.

    And yes, I know there are builtin methods to merge dictionaries (update),
    but I wanted finer control over handling for existing keys.

    Args:
        target (dictionary): The destination dictionary.
        new_items (dictionary): The dictionary of new items to merge.

    Returns:
        dict: target, with all unique items merged from new items.
    """

    for name, data in new_items.items():
        if name in target:
            # TODO: add logging
            # logging.getLogger(__name__).warn(
            # 'Dictionary item %s already defined', name)
            pass
        else:
            target[name] = data

    return target
