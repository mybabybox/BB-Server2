(function($)
{

	Pace.once('done', function()
	{
		// restore visibility
		$('.container-fluid').css('visibility', 'visible').show();
		$(window).trigger('load');
	});

})(jQuery);