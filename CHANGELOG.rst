Changelog
=========

0.1.0 (2015-06-23)
------------------
* Initial commit of core Sambuca code after split from single package.

1.0.0 (2015-12-01)
------------------
* Data loading functions for sensor filters, and general spectra implemented.
  Supported formats are ENVI spectral libraries, and tabular data in Excel and
  CSV formats.
* Full physical model added to the forward model.

1.1.0 (2015-12-03)
------------------
* Additional SIOP and IOP outputs added to forward model. These values were
  being calculated anyway, and they were required by Bioopti.
* Docstrings are now almost complete.
* Some naming inconsistencies have been addressed.

1.2.0 (2015-12-04)
------------------
* forward model changes:

  * renamed slope_cdom to a_cdom_slope to indicate that it is an absorption
    value, and for consistency with the main naming convention.
  * renamed slope_nap to a_nap_slope for the same reasons.
  * split backscatter slope into two values, one for phytoplankton, and one for
    NAP. Both are optional, and if the NAP backscatter slope value is not
    supplied, the phytoplankton backscatter slope value will be reused. This
    preserves the behaviour of the IDL model which only has a single backscatter
    slope value for both phytoplankton and NAP. The new slope parameter names
    conform to the primary naming convention.

1.2.1 (2015-12-04)
------------------
* Implemented q_factor and R(0-) calculations.

1.2.2 (2015-12-04)
------------------
* Fixed filename case-sensitivity issue between Windows and Linux.

1.3.0 (2018-06-18)
------------------
* Added licence, cleaned up docs and pushed to GitHub.

1.3.1 (2018-06-18)
------------------
* Added Readthedocs link.
* updated changelog.

1.3.2 (Wednesday June 13, 2018)
------------------
* Minor documentation fixes.
* Setup Travis CI integration.
* Completed Read the Docs integration.
* Removed Python 3.4 from CI, as it is not supported by pandas.

1.3.3 (Wednesday June 13, 2018)
------------------
* Updating setup.py metadata
* First version pushed to PyPI
