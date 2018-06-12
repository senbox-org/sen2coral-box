SHELL=/bin/sh

.SILENT:
.IGNORE:

.PHONY: help
help:
	echo
	echo 'Utility Makefile for Sambuca Core'
	echo '============================'
	echo
	echo 'Targets supported are:'
	echo
	echo '  * clean: removes the build and htmlcov (coverage reports) directories, as well as __pycache__ and *.pyc files. Note that a clean also removes the generated documentation (as this is placed into build/docs).'
	echo '  * sitepkg-develop: Use this only when you are using a virtual environment created with the --system-site-packages flag. It forcibly installs some packages into the virtual environment to work around issues where a package and its plugins are installed to different locations'
	echo '  * install-deps: installs development and test dependencies into your virtual environment.'
	echo '  * develop: installs sambuca_core in development mode.'
	echo '  * test: runs the unit tests.'
	echo '  * lint: runs pylint.'
	echo '  * html: builds the HTML documentation.'
	echo '  * pdf: builds the documentation in PDF format.'
	echo '  * latex: builds LaTeX source, used to generate other formats.'
	echo '  * alldocs: builds all documentation formats.'
	echo '  * sdist: builds a source distribution.'
	echo '  * bdist_wheel: builds a universal wheel distribution.'

.PHONY: test
test:
	py.test

.PHONY: clean
clean:
	echo Cleaning ...
	rm -rf build/
	find ./sambuca_core/ -name "__pycache__" -exec rm -rf {} \;
	find ./sambuca_core/ -name "*.pyc" -exec rm -rf {} \;
	echo ... done

.PHONY: install-deps
install-deps:
	pip install -e.[dev,test]

.PHONY: develop
develop: install-deps
	python setup.py develop

#--system-site-packages plugin issue workaround
# To work correctly with virtual environments, sphinx, pytest, and all their
# plugins must be installed into the virtual environment even if they are
# already available in the site packages.
# If additional plugins to sphinx and pytest are added to setup.py, they must
# also be added here.
.PHONY: sitepkg-develop
sitepkg-develop: develop
	pip install --ignore-installed \
		sphinx \
		pytest pytest-sugar \
		ipython ipdb

.PHONY: lint
lint:
	pylint ./sambuca_core/

.PHONY: sdist
sdist:
	python setup.py sdist

.PHONY: bdist_wheel
bdist_wheel:
	python setup.py bdist_wheel

.PHONY: html
html:
	sphinx-build -b html docs build/docs/html

.PHONY: latex
latex:
	sphinx-build -b latex docs build/docs/latex

pdf: latex
	$(MAKE) -C build/docs/latex all-pdf
	mkdir -p ./build/docs/pdf/
	mv build/docs/latex/*.pdf build/docs/pdf/

alldocs: html latex pdf

