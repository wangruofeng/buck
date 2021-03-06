from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
from __future__ import with_statement

from . import util

import unittest


class UtilTest(unittest.TestCase):
    def test_is_in_dir(self):
        self.assertTrue(util.is_in_dir('foo/bar.py', 'foo'))
        self.assertTrue(util.is_in_dir('foo/bar.py', 'foo/'))
        self.assertTrue(util.is_in_dir('/foo/bar.py', '/'))
        self.assertFalse(util.is_in_dir('foo.py', 'foo'))
        self.assertFalse(util.is_in_dir('foo/bar.py', 'foo/bar'))
        self.assertFalse(util.is_in_dir('foo/bars', 'foo/bar'))


if __name__ == '__main__':
    unittest.main()
