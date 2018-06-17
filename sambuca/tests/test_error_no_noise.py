from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)
from builtins import *

from pkg_resources import resource_filename

import numpy as np
from scipy.io import readsav

import sambuca as sb


class TestErrorNoNoise(object):
    """ Error function tests, without noise. """

    def setup_method(self, method):
        self.data = readsav(
            resource_filename(
                sb.__name__,
                'tests/data/no_noise_error_data.sav'))

    def test_validate_data(self):
        observed = self.data['realrrs']
        modelled = self.data['rrs']

        assert len(modelled) == len(observed)

    def test_error_all(self):
        observed = self.data['realrrs']
        modelled = self.data['rrs']
        expected_distance_a = self.data['error_a']
        expected_distance_af = self.data['error_af']
        expected_distance_f = self.data['error_f']
        expected_lsq = self.data['lsq']

        actual = sb.error_all(observed, modelled)

        assert np.allclose(actual.alpha, expected_distance_a)
        assert np.allclose(actual.alpha_f, expected_distance_af)
        assert np.allclose(actual.f, expected_distance_f)
        assert np.allclose(actual.lsq, expected_lsq)

    def test_distance_alpha(self):
        observed = self.data['realrrs']
        modelled = self.data['rrs']
        expected = self.data['error_a']

        actual = sb.distance_alpha(observed, modelled)

        assert np.allclose(actual, expected)

    def test_distance_alpha_f(self):
        observed = self.data['realrrs']
        modelled = self.data['rrs']
        expected = self.data['error_af']

        actual = sb.distance_alpha_f(observed, modelled)

        assert np.allclose(actual, expected)

    def test_distance_f(self):
        observed = self.data['realrrs']
        modelled = self.data['rrs']
        expected = self.data['error_f']

        actual = sb.distance_f(observed, modelled)

        assert np.allclose(actual, expected)

    def test_distance_lsq(self):
        observed = self.data['realrrs']
        modelled = self.data['rrs']
        expected = self.data['lsq']

        actual = sb.distance_lsq(observed, modelled)

        assert np.allclose(actual, expected)
