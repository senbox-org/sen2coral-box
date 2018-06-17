============
Contributing
============

Contributions are welcome, and they are greatly appreciated! Every
little bit helps, and credit will always be given.

Bug reports
-----------

When `reporting a bug <https://github.com/csiro-aquatic-remote-sensing/sambuca/issues>`_ please include:

    * Your operating system name and version.
    * Any details about your local setup that might be helpful in troubleshooting.
    * The Sambuca package version.
    * Detailed steps to reproduce the bug.

Documentation improvements
--------------------------

Sambuca could always use more documentation, whether as part of the
official Sambuca docs, in docstrings, or even on the web in blog posts,
articles, and such.

.. note:: This project uses Google-style docstrings.
   Contributed code should follow the same conventions.
   For examples, please see the `Napoleon examples
   <http://sphinxcontrib-napoleon.readthedocs.org/en/latest/example_google.html>`_,
   or the `Google Python Style Guide
   <http://google-styleguide.googlecode.com/svn/trunk/pyguide.html>`_.


Feature requests and feedback
-----------------------------

The best way to send feedback is to `file an issue <https://github.com/csiro-aquatic-remote-sensing/sambuca/issues>`_

If you are proposing a feature:

* Explain in detail how it would work.
* Keep the scope as narrow as possible, to make it easier to implement.
* Remember that this is a volunteer-driven project, and that contributions are welcome :)

Or, implement the feature yourself and submit a pull request.

Development
-----------

1. Fork the `sambuca` repo on GitHub.
2. Clone your fork locally.
3. Create a feature branch.
4. When you're done making changes, run all the tests, doc builder and pylint
   checks::

    py.test
    tox
    pylint ./src/sambuca/
    sphinx-build -b html docs build/docs

   Or, using the project makefile::

    make clean lint tests html
    tox

5. Commit your changes and push your branch to GitHub.
6. Submit a pull request through the GitHub website.

There is a makefile in the project root with targets for the most common
development operations such as lint checks, running unit tests, building the
documentation, and installing packages.

`Bumpversion <https://pypi.python.org/pypi/bumpversion>`_ is used to manage the
package version numbers. This ensures that the version number is correctly
incremented in all required files. Please see the bumpversion documentation for
usage instructions, and do not edit the version strings directly.

.. note:: Sphinx requires a working LaTeX installation to build the pdf documentation.

Pull Request Guidelines
-----------------------

If you need some code review or feedback while you're developing the code just make the pull request.

For merging, you should:

1. Include passing tests (run ``py.test``).
2. Update documentation when there's new API, functionality etc.
3. Add a note to ``CHANGELOG.rst`` about the changes.
4. Add yourself to ``AUTHORS.rst``.
