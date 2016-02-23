
var dismissAppDownloadTips = function() {
	$('#appDownloadTips').hide();
	sessionStorage.setItem('dismissAppDownloadTips','true');
	
	// use sessionStorage for current session only
	//localStorage.setItem('dismissAppDownloadTips','true');
}

var showAppDownloadTips = function() {
	// use sessionStorage for current session only
	//var dismiss = localStorage.getItem('dismissAppDownloadTips');
	
	var dismiss = sessionStorage.getItem('dismissAppDownloadTips');
	//if ((isAndroid() || isIOS()) && dismiss != 'true') {
	if (isAndroid() && dismiss != 'true') {
		return true;
	}
	return false;
}

var getAppDownloadUrl = function() {
	if (isAndroid()) {
		return "https://play.google.com/store/apps/details?id=com.babybox.app";
	} else if (isIOS()) {
		return ""
	}
	return "";
}

//
// write meta
//
var META_DESCRIPTION_CHAR_LIMIT = 150;

var writeMetaCanonical = function(absUrl) {
	//log('url=: '+absUrl);
	$('link[rel=canonical]').attr('href', absUrl);
}

var writeMetaTitleDescription = function(title, description, image) {
	title = title + " | BabyBox 媽媽即拍即賣";
	document.title = title;
	$('meta[name=description]').attr('content', description.substring(0,META_DESCRIPTION_CHAR_LIMIT));
	$('meta[name=keywords]').attr('content', title + ', ' + $('meta[name=keywords]').attr('content'));
	if (image && image != undefined) {
		$('meta[property="og:image"]').attr('content', image);
	}
}

var getMetaForSchool = function(obj) {
	var title, description;
	
	var name = obj.n;
	if (obj.ne && obj.ne != undefined) {
		name += ' ' + obj.ne;
	}
	title = name + ' 收生與收費資料';
	
	var schoolType = '';
	var hasPN = '';
	if (obj.hasPN == undefined) {
		schoolType = '幼兒班';
	} else {
		schoolType = '幼稚園';
		hasPN = (obj.hasPN? '有' : '沒有') + '提供幼兒班，';
	}
	
	description = name + ' 是一所' + 	//schoolType + '概覽：' + 
		obj.orgt + '學校，' +
		(obj.cp? '可' : '不可') + '兌現學券，' +
		hasPN + 
		'位於' + obj.dis + '。' +
		'立即查看更多收生與收費資料。';
	
	return {
		title: title,
		description: description
	}
}

//
// Hooks to detect browser activeness 
// for header meta refresh
//

window.isBrowserTabActive = false;

$(window).focus(function () {
    window.isBrowserTabActive = true; 
}); 

$(window).blur(function () {
    window.isBrowserTabActive = false; 
});

$(window).on("blur focus", function(e) {
    var prevType = $(this).data("prevType"); // getting identifier to check by
    if (prevType != e.type) {   //  reduce double fire issues by checking identifier
        switch (e.type) {
            case "blur":
                window.isBrowserTabActive = false; 
                break;
            case "focus":
                window.isBrowserTabActive = true; 
                break;
        }
    }
    $(this).data("prevType", e.type); // reset identifier
})

//
// Common
//

var isAndroid = function() {
	if (/Android/i.test(navigator.userAgent)) {
		return true;
	}
	return false;
}

var isIOS = function() {
	if (/iPhone|iPad|iPod/i.test(navigator.userAgent)) {
		return true;
	}
	return false;
}

var isMobile = function() {
	if (/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)) {
		return true;
	}
	return false;
}

var startsWith = function(str, s) {
	return str.indexOf(s, 0) === 0;
	//return str.slice(0, s.length) == s;
}

var highlightLink = function(id) {
    $('#'+id).select();
}

var formatToExternalUrl = function(url) {
    if (!startsWith(url,'http')) {
    	if (!startsWith(url,'/')) {
    		url = '/' + url;
    	}
    	url = DefaultValues.BASE_URL + url;
    }
    return url;
}

//
// Simple log helper
//

var log = function(str) {
    if (this.console)
        console.log(str);
}

var debugObject = function(obj, console) {
    if (console) {
        log(JSON.stringify(obj));
    } else {
        alert(JSON.stringify(obj));
    }
}

//
// header bottom glow
//

var toggleToNotVisible = true;
var mainLogoNotVisible = true;

$(window).scroll(function() {
    if ($('#main-top').length > 0 && $('#main-top').visible(true)) {
        //console.log("header no glow");
        $('#header-backdrop').removeClass('header-glow');
        toggleToNotVisible = true;
        
        // show main logo
        $('#main-logo').slideDown(300,'swing');
    } else if (toggleToNotVisible) {
        //console.log("header glow");
        $('#header-backdrop').addClass('header-glow');
        toggleToNotVisible = false;
    }
    
    if ($('#header-menu').length > 0 && $('#header-menu').visible(true)) {
        mainLogoNotVisible = true;
    } else if (mainLogoNotVisible) {
        // hide main logo
        $('#header-logo').hide();
        $('#header-logo').show(500);
        $('#main-logo').slideUp(300,'swing');
        mainLogoNotVisible = false;
    }
});

//
// http://stackoverflow.com/questions/22964767/how-to-format-angular-moments-am-time-ago-directive
// http://jsbin.com/qeweyalu/1/edit
// am-time-ago date format
//

moment.lang('en', {
    relativeTime : {
        future: "在 %s",
        past:   "%s",
        s:  "剛剛",   //"%d秒",
        m:  "1分鐘前",
        mm: "%d分鐘前",
        h:  "1小時前",
        hh: "%d小時前",
        d:  "昨天",
        dd: "%d日前",
        M:  "1個月前",
        MM: "%d個月前",
        y:  "1年前",
        yy: "%d年前"
    }
});

//
// Utility function to convert to real links
//

var convertText = function(text) {
    var escaped = escapeHtmlSpecialChars(text);
    return convertToLinks(convertEmoticons(escaped));
}

var escapeHtmlSpecialChars = function(text) {
    return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;');
}

var convertEmoticons = function(text) {
    // TODO
    return text;
}

var convertToLinks = function(text) {
    var replacedText, replacePattern1, replacePattern2;

    //"http(s)://"
    //replacePattern1 = /(\b(https?):\/\/[-A-Z0-9+&amp;@#\/%?=~_|!:,.;]*[-A-Z0-9+&amp;@#\/%=~_|])/ig;
    replacePattern1 = /(\b(https?):\/\/.*[-A-Z0-9+&amp;@#\/%=~_|])/ig;
    replacedText = text.replace(replacePattern1, '<a href="$1" target="_blank">$1</a>');

    //"www."
    replacePattern2 = /(^|[^\/])(www\.[\S]+(\b|$))/gim;
    replacedText = replacedText.replace(replacePattern2, '$1<a href="http://$2" target="_blank">$2</a>');

    return replacedText;
}

//
// bootbox
//

var prompt = function(message, className, timeout) {
    if (className == undefined || className.length == 0) {
        className = "bootbox-default-prompt";
    }
    
    var buttons = {
            /*main: {
                label: "Copy",
                className: "btn-default",
                callback: function() {
                    $('.bootbox-body').select();
                }
            },*/
            success: {
                label: "OK",
                className: "btn-primary",
                callback: function() {
                }
            }
        };
    
    
    if (timeout != undefined && timeout > 0) {
        window.setTimeout(function(){
            bootbox.hideAll();
        }, timeout);
        buttons = {};
    }
    
    bootbox.dialog({
        message: message,
        title: "",
        className: className,
        buttons: buttons
    });
}

//
// Translate validation messages
//
var translateValidationMessages = function() {
    var elements = document.getElementsByTagName("INPUT");
    for (var i = 0; i < elements.length; i++) {
        elements[i].oninvalid = function(e) {
            e.target.setCustomValidity("");
            e.target.checkValidity();
            e.target.setCustomValidity(__Msg(e.target.validationMessage));
        };
    }
}

// translate messages
function __Msg(str){
    if (str.indexOf("fill out this field") != -1)
        return "請填寫此欄。";
    else if (str.indexOf("email address") != -1)
        return "請填寫電郵。";
    return str; // skip not translated messages
}
