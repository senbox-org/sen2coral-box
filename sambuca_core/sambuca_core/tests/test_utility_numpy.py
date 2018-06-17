# -*- coding: utf-8 -*-
# Ensure compatibility of Python 2 with Python 3 constructs
from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)

from ..utility.numpy import strictly_increasing, strictly_decreasing

def test_strictly_increasing_true():
    x = [-2, -1.9, 0, 3, 4, 9]
    assert strictly_increasing(x)

def test_strictly_increasing_false():
    x = [-2, -1.9, 3, 0, 4, 9]
    assert not strictly_increasing(x)

def test_strictly_increasing_false_on_monotonic_increasing_sequence():
    x = [-2, -1.9, 3, 3, 4, 9]
    assert not strictly_increasing(x)

def test_strictly_decreasing_true():
    x = [83, 43.9, 24.83, 9, -13]
    assert strictly_decreasing(x)

def test_strictly_decreasing_false():
    x = [83, 143.9, 24.83, 9, -13]
    assert not strictly_decreasing(x)

def test_strictly_decreasing_false_on_monotonic_decreasing_sequence():
    x = [123, 99, 43, 42, 42, 9, 1]
    assert not strictly_decreasing(x)
