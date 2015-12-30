'use strict';

angular.module('babybox', [
  'infinite-scroll',
  'ngResource',
  'ngRoute',
  'ngDialog',
  'xeditable',
  'ui.bootstrap',
  'ui.bootstrap.tpls',
  'angularFileUpload',
  'ui.bootstrap.datetimepicker',
  'validator',
  'validator.rules',
  'angularSpinner',
  'truncate',
  'ui.tinymce',
  'ui.utils',
  'ngSanitize',
  'angularMoment',
  'pascalprecht.translate',
  'ngCookies'
])
.config(function ($routeProvider, $locationProvider) {
	/*
	$routeProvider
		.when('/', {
			templateUrl: '/assets/app/views/babybox/web/home.html'
		})
		.otherwise({
			redirectTo: '/'
		});
	*/
    $locationProvider
      	.html5Mode(false)
      	.hashPrefix('!');
})
.run(function(editableOptions) {
	editableOptions.theme = 'bs3'; // bootstrap3 theme. Can be also 'bs2', 'default'
});

//
// noCache for browser
//

var babybox = angular.module('babybox');

var URL_IGNORE = [
    "tracking",
    "template", 
    "assets", 
    "image", 
    "photo", 
    "modal"
];

babybox.config(['$httpProvider', function($httpProvider) {
    $httpProvider.interceptors.push('noCacheInterceptor');
    }]).factory('noCacheInterceptor', function () {
            return {
                request: function (config) {
                    //console.log(config.method + " " + config.url);
                    if(config.method=='GET'){
                        var url = config.url.toLowerCase();
                        var containsUrlIgnore = false;
                        for (var i in URL_IGNORE) {
                            if (url.indexOf(URL_IGNORE[i]) != -1) {
                                containsUrlIgnore = true;
                            }
                        }
                        if (!containsUrlIgnore) {
                            var separator = config.url.indexOf('?') === -1 ? '?' : '&';
                            config.url = config.url+separator+'noCache=' + new Date().getTime();
                            //console.log(config.method + " " + config.url);
                        }
                    }
                    return config;
               }
           };
    });
    
babybox.config(['$translateProvider', function ($translateProvider) {
	$translateProvider.translations('zh', {
		// Home page
	    "HOME_EXPLORE":"逛逛",
	    "HOME_FOLLOWING":"關注",
	    // Category page
	    "POPULAR":"熱門商品",
	    "NEWEST":"最新上架",
	    // Product page
	    "LIKE":"喜歡",
	    "COMMENTS":"留言",
	    "PRODUCT_DETAILS":"商品詳情",
	    "CONDITION":"商品狀況",
	    "CATEGORY":"商品分類",
	    "VIEW_SELLER":"看看商店",
	    "SELLER_FOLLOWERS":"關注",
	    "VIEW_ALL":"看所有",
	    "YOU_MAY_ALSO_LIKE":"你可能喜歡",
	    "CHAT_NOW":"聯絡賣家",
	    "BUY_NOW":"購買",
	    // Profile page
	    "FOLLOW":"+ 關注",
	    "FOLLOWING":"關注中",
	    "FOLLOWERS":"關注我",
	    "FOLLOWINGS":"關注中",
	    "PRODUCTS":"商品",
	    "LIKES":"喜歡",
	    "LOGOUT":"登出",
	    // Other
	    "APP_DOWNLOAD_MESSAGE":"免費下載 BabyBox 手機 App。 購買商品或刊登你的商品 (30秒完成刊登)。 媽媽即拍即賣！",
	    "APP_IOS_COMING_SOON":"BabyBox iPhone 手機 App 快將推出",
	    "DOWNLOAD_NOW":"立即下載"
	});
	
	$translateProvider.translations('en', {
		// Home page
		"HOME_EXPLORE":"Explore",
	    "HOME_FOLLOWING":"Following",
	    // Category page
	    "POPULAR":"Popular",
	    "NEWEST":"Newest",
	    // Product page
	    "LIKE":"Like",
	    "COMMENTS":"Comments",
	    "PRODUCT_DETAILS":"Product Details",
	    "CONDITION":"Condition",
	    "CATEGORY":"Category",
	    "VIEW_SELLER":"View Shop",
	    "SELLER_FOLLOWERS":"Followers",
	    "VIEW_ALL":"View all",
	    "YOU_MAY_ALSO_LIKE":"You may also like",
	    "CHAT_NOW":"Chat Now",
	    "BUY_NOW":"Buy Now",
	    // Profile page
	    "FOLLOW":"+ Follow",
	    "FOLLOWING":"Following",
	    "FOLLOWERS":"Followers",
	    "FOLLOWINGS":"Following",
	    "PRODUCTS":"Products",
	    "LIKES":"Likes",
	    "LOGOUT":"Logout",
	    // Other
	    "APP_DOWNLOAD_MESSAGE":"Download BabyBox for free now and list your product in 30 seconds. Everymom is a Seller!",
	    "APP_IOS_COMING_SOON":"BabyBox iPhone App is coming very soon!",
	    "DOWNLOAD_NOW":"Download Now"
	});
	 
	$translateProvider.preferredLanguage('zh');
	//$translateProvider.useLocalStorage();		// use cookies setting
}]);


//babybox.config(['$httpProvider', function($httpProvider) {
//    if (!$httpProvider.defaults.headers.get) {
//        $httpProvider.defaults.headers.get = {};    
//    }
//    $httpProvider.defaults.headers.get['If-Modified-Since'] = '0';
//    $httpProvider.defaults.headers.get['Cache-Control'] = 'no-cache'; 
//    $httpProvider.defaults.headers.get['Pragma'] = 'no-cache';
//}]);