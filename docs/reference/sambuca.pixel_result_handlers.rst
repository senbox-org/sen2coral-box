Pixel Result Handlers
=====================

Pixel result handlers are called by the parameter estimator with the final
result for a pixel. Subclasses can implement various functionality, such as
writing results to memory structures or files.

.. autoclass:: sambuca.pixel_result_handler.PixelResultHandler
.. autoclass:: sambuca.array_result_writer.ArrayResultWriter
