# -*- coding: utf-8 -*-
# Ensure compatibility of Python 2 with Python 3 constructs
from __future__ import (
    absolute_import,
    division,
    print_function,
    unicode_literals)
from builtins import *

from pkg_resources import resource_filename

import numpy as np
import pytest
import sambuca_core as sbc
from scipy.io import readsav


# @skip
class TestForwardModel(object):

    """Sambuca forward model test class
    """

    @classmethod
    def setup_class(cls):
        # load the test values
        filename = resource_filename(
            sbc.__name__,
            './tests/data/forward_model_test_data.sav')
        cls.data = readsav(filename)
        cls.unpack_parameters()
        cls.unpack_spectra()
        cls.unpack_input_params()
        cls.unpack_results()
        cls.unpack_substrates()
        # TODO: Why are my outputs not closer? IDL data appears to read in as single precision.
        cls.rtol = 1e-3
        cls.atol = 5e-4
        cls.q_factor = np.pi

    @classmethod
    def unpack_parameters(cls):
        # The IDL code has the parameters packed into a structure called ZZ.
        # Magic numbers here are drawn directly from the IDL code.
        zz = cls.data.zz
        cls.chl = zz[1]
        cls.cdom = zz[2]
        cls.nap = zz[3]
        cls.x_ph_lambda0x = zz[4]
        cls.x_nap_lambda0x = zz[5]
        cls.slope_cdom = zz[6]
        cls.slope_nap = zz[7]
        cls.a_nap_lambda0nap = zz[8]
        cls.slope_backscatter = zz[9]
        cls.q1 = zz[10]
        cls.q2 = zz[11]
        cls.q3 = zz[12]
        cls.H = zz[13]

    @classmethod
    def unpack_spectra(cls):
        s = cls.data.sambuca.input_spectra[0]
        cls.wav = s.wl[0]
        cls.a_water = s.awater[0]
        cls.bb_water = s.bbwater[0]
        cls.a_ph_star = s.aphy_star[0]
        cls.a_cdom_star = s.acdom_star[0]
        cls.a_nap_star = s.atr_star[0]
        cls.bb_ph_star = s.bbph_star[0]
        cls.bb_nap_star = s.bbtr_star[0]

    @classmethod
    def unpack_input_params(cls):
        p = cls.data.sambuca.input_params[0]
        cls.theta_air = p.theta_air
        cls.lambda0cdom = p.lambda0cdom
        cls.lambda0nap = p.lambda0tr
        cls.lambda0x = p.lambda0x

    @classmethod
    def unpack_substrates(cls):
        spectra = cls.data.sambuca.inputr[0].spectra[0]
        # it appears that in the test I set up, the substrates are both the same
        cls.substrate1 = spectra[:,0]
        cls.substrate2 = spectra[:,1]

    @classmethod
    def unpack_results(cls):
        r = cls.data.spectra
        cls.expected_substrate_r = r.substrater[0]
        cls.expected_rrs = r.rrs[0]
        cls.expected_rrsdp = r.rrsdp[0]
        cls.expected_kd = r.kd[0]
        cls.expected_kub = r.kub[0]
        cls.expected_kuc = r.kuc[0]
        cls.expected_a = r.a[0]
        cls.expected_bb = r.bb[0]

    def test_validate_data(self):
        assert self.data
        assert len(self.data.zz) == 15

        spectra = [
            self.wav,
            self.a_water,
            self.a_ph_star,
            self.expected_substrate_r,
            self.expected_rrs,
            self.expected_rrsdp,
            self.expected_kd,
            self.expected_kub,
            self.expected_kuc,
            self.substrate1,
            self.substrate2,
        ]
        for array in spectra:
            assert len(array) == 551

        assert self.lambda0cdom == 440

    def run_forward_model(self):
        return sbc.forward_model(
            self.chl,
            self.cdom,
            self.nap,
            self.H,
            self.substrate1,
            self.wav,
            self.a_water,
            self.a_ph_star,
            551,
            substrate_fraction=self.q1,
            substrate2=self.substrate2,
            x_ph_lambda0x=self.x_ph_lambda0x,
            x_nap_lambda0x=self.x_nap_lambda0x,
            a_cdom_slope=self.slope_cdom,
            a_nap_slope=self.slope_nap,
            a_nap_lambda0nap=self.a_nap_lambda0nap,
            bb_ph_slope=self.slope_backscatter,
            bb_nap_slope=self.slope_backscatter,
            lambda0cdom=self.lambda0cdom,
            lambda0nap=self.lambda0nap,
            theta_air=self.theta_air,
            water_refractive_index=1.333,  # The hard-coded IDL value
            # self.off_nadir,
            q_factor=self.q_factor,
        )

    def test_substrate_r(self):
        results = self.run_forward_model()
        assert np.allclose(results.r_substratum, self.expected_substrate_r)

    def test_default_substrate_fraction(self):
        # the default substrate_fraction should give r_substratum == substrate1
        results = sbc.forward_model(
            self.chl,
            self.cdom,
            self.nap,
            self.H,
            self.substrate1,
            self.wav,
            self.a_water,
            self.a_ph_star,
            551,
            substrate2=self.substrate2)

        assert np.allclose(results.r_substratum, self.substrate1)

    def test_substrate_r_single_substrate(self):
        # set the second substrate to None, and adjust the expectation that
        # results.r_substratum will equal substrate1
        self.substrate2 = None
        results = self.run_forward_model()
        assert np.allclose(results.r_substratum, self.substrate1)

    def test_rrs(self):
        results = self.run_forward_model()
        assert np.allclose(
            results.rrs,
            self.expected_rrs,
            atol=self.atol,
            rtol=self.rtol)

    def test_rrs_deep(self):
        results = self.run_forward_model()
        assert np.allclose(
            results.rrsdp,
            self.expected_rrsdp,
            atol=self.atol,
            rtol=self.rtol)

    def test_r_0_minus(self):
        self.q_factor = 3.5123
        results = self.run_forward_model()
        expected_r_0_minus = results.rrs * self.q_factor
        assert np.allclose(
            results.r_0_minus,
            expected_r_0_minus,
            atol=self.atol,
            rtol=self.rtol)

    def test_r_0_minus_deep(self):
        self.q_factor = np.pi
        results = self.run_forward_model()
        expected_rdp_0_minus = results.rrsdp * self.q_factor
        assert np.allclose(
            results.rdp_0_minus,
            expected_rdp_0_minus,
            atol=self.atol,
            rtol=self.rtol)

    def test_kd(self):
        results = self.run_forward_model()
        assert np.allclose(
            results.kd,
            self.expected_kd,
            atol=self.atol,
            rtol=self.rtol)

    def test_kub(self):
        results = self.run_forward_model()
        assert np.allclose(
            results.kub,
            self.expected_kub,
            atol=self.atol,
            rtol=self.rtol)

    def test_kuc(self):
        results = self.run_forward_model()
        assert np.allclose(
            results.kuc,
            self.expected_kuc,
            atol=self.atol,
            rtol=self.rtol)

    def test_a(self):
        results = self.run_forward_model()
        assert np.allclose(
            results.a,
            self.expected_a,
            atol=self.atol,
            rtol=self.rtol)

    def test_bb(self):
        results = self.run_forward_model()
        assert np.allclose(
            results.bb,
            self.expected_bb,
            atol=self.atol,
            rtol=self.rtol)

    def test_a_cdom_star(self):
        results = self.run_forward_model()
        assert np.allclose(
            results.a_cdom_star,
            self.a_cdom_star,
            atol=self.atol,
            rtol=self.rtol)

    def test_a_nap_star(self):
        results = self.run_forward_model()
        assert np.allclose(
            results.a_nap_star,
            self.a_nap_star,
            atol=self.atol,
            rtol=self.rtol)

    def test_bb_water(self):
        results = self.run_forward_model()
        assert np.allclose(
            results.bb_water,
            self.bb_water,
            atol=self.atol,
            rtol=self.rtol)

    def test_bb_ph_star(self):
        results = self.run_forward_model()
        assert np.allclose(
            results.bb_ph_star,
            self.bb_ph_star,
            atol=self.atol,
            rtol=self.rtol)

    def test_bb_nap_star(self):
        results = self.run_forward_model()
        assert np.allclose(
            results.bb_nap_star,
            self.bb_nap_star,
            atol=self.atol,
            rtol=self.rtol)

    def test_a_ph(self):
        # Derive the phytoplankton absorption from the specific absorption
        # and chl concentration inputs.
        # a_ph = chl * aphy*
        expected_a_ph = self.chl * self.a_ph_star
        results = self.run_forward_model()
        assert np.allclose(
            results.a_ph,
            expected_a_ph)

    def test_a_cdom(self):
        # I don't have a_cdom data from IDL, but I know what it should be.
        # a_cdom = a_cdom* * cdom
        results = self.run_forward_model()
        expected_a_cdom = results.a_cdom_star * self.cdom
        assert np.allclose(
            results.a_cdom,
            expected_a_cdom)

    def test_a_nap(self):
        # I don't have a_nap data from IDL, but I know what it should be.
        # a_nap = a_nap* * nap
        results = self.run_forward_model()
        expected_a_nap = results.a_nap_star * self.nap
        assert np.allclose(
            results.a_nap,
            expected_a_nap)

    def test_bb_ph(self):
         # expected bb_ph = bb_ph_star * chl
        expected_bb_ph = self.chl * self.bb_ph_star
        results = self.run_forward_model()
        assert np.allclose(
            results.bb_ph,
            expected_bb_ph,
            atol=self.atol,
            rtol=self.rtol)

    def test_bb_nap(self):
        # expected bb_nap = bb_nap_star * nap
        expected_bb_nap = self.nap * self.bb_nap_star
        results = self.run_forward_model()
        assert np.allclose(
            results.bb_nap,
            expected_bb_nap,
            atol=self.atol,
            rtol=self.rtol)

    def test_total_absorption(self):
        results = self.run_forward_model()
        expected_a = self.a_water + results.a_ph + results.a_cdom + results.a_nap
        assert np.allclose(
            results.a,
            expected_a,
            atol=self.atol,
            rtol=self.rtol)

    def test_total_backscatter(self):
        results = self.run_forward_model()
        expected_bb = results.bb_water + results.bb_ph + results.bb_nap
        assert np.allclose(
            results.bb,
            expected_bb,
            atol=self.atol,
            rtol=self.rtol)
