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

from os import listdir
from os.path import normcase, isfile, join, splitext


def list_files(directory, extensions=None):
    """ Get a list of files in a directory, filtered by an optional list
    of extensions.

    Args:
        directory (str): The directory to list.
        extensions (list): Optional list of file extensions.

    Returns:
        list: The list of matching file info objects.
    """

    # Get the raw directory listing of matching files
    file_list = [join(directory, normcase(f)) for f in listdir(directory) if
                 isfile(join(directory, f))]

    # if we have an extension filter, then apply it
    if extensions:
        file_list = [f for f in file_list if splitext(f)[1][1:] in extensions]

    return file_list
