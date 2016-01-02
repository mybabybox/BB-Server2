'use strict';

var babybox = angular.module('babybox');

babybox.controller('HomeController', 
		function($scope, $translate, $location, $route, categoryService, productService, $rootScope, ngDialog, userInfo, $anchorScroll, usSpinnerService) {
	
	writeMetaCanonical($location.absUrl());
	
	usSpinnerService.spin('loading...');
	
	$scope.userInfo = userInfo;
	$scope.homeFeed=true;
	
	$scope.products = productService.getHomeExploreFeed.get({offset:0});
	$scope. getHomeExploreProducts= function () {
		$scope.products = productService.getHomeExploreFeed.get({offset:0});
		$scope.homeFeed=true;
		$scope.noMore = true;
	};
	
	$scope.getHomeFollowingProducts = function () {
		$scope.products = productService.getHomeFollowingFeed.get({offset:0});
		$scope.homeFeed=false;
		$scope.noMore = true;
	};

	$scope.categories = categoryService.getAllCategories.get();

	var flag = true;
	$scope.noMore = true;
	$scope.loadMore = function () {
		if(($scope.products.length!=0) && ($scope.noMore==true) && flag == true){

			var len = $scope.products.length;
			var off = $scope.products[len-1].offset;
			if($scope.homeFeed){
				flag = false;
				productService.getHomeExploreFeed.get({offset:off}, function(data){
					if(data.length == 0) {
						$scope.noMore = false;
					}
					angular.forEach(data, function(value, key) {
						if(!flag) {
							$scope.products.push(value);
						}
					});
					flag=true;
				});
			}
			if(!$scope.homeFeed){
				flag = false;
				productService.getHomeFollowingFeed.get({offset:off}, function(data){
					if(data.length == 0) {
						$scope.noMore = false;
					}
					angular.forEach(data, function(value, key) {
						if(!flag)
							$scope.products.push(value);
					});
					flag=true;
				});
			}
		}		
	}
	
	// UI helper
	$(window).scroll(function(e){
		$scope.position = window.pageYOffset;
		if($scope.position > 750) {
			$("#back-to-top").show();
		} else {
			$("#back-to-top").hide();
		}
	});
	
	$scope.gotoTop=function(){
		$location.hash('');
		$anchorScroll();
	};
});

babybox.controller('CategoryPageController', 
		function($scope, $location, $translate, $route, $rootScope, ngDialog, $routeParams,userInfo, category, product, categoryService, $anchorScroll, usSpinnerService) {
	
	writeMetaCanonical($location.absUrl());
	
	/*
	writeMetaTitleDescription(
			category.name, 
			category.description, 
			formatToExternalUrl(category.icon));
	*/
	
	usSpinnerService.spin('loading...');
	
	$scope.userInfo = userInfo;
	$scope.products = product;
	//console.log($scope.products);
	
	$scope.cat = category;
	var catid = $scope.cat.id;
	
	//we are routing this from scala file so we can't able to get $routeParams , so this is just a workaround
	var url = $location.absUrl();
	var values= url.split("/");
	$scope.catType = values[values.length-1];

	if($scope.catType == 'popular')
		$scope.noMore = true;
	if($scope.catType == 'newest')
		$scope.noMore = true;
	if($scope.catType == 'high2low')
		$scope.noMore = true;
	if($scope.catType == 'low2high')
		$scope.noMore = true;
	
	$scope.gotoTop=function(){
		$location.hash('');
		$anchorScroll();
	};
	
	var catid = $scope.cat.id;
	var flag = true;
	
	$scope.loadMore = function () {
		if(($scope.products.length!=0) && ($scope.noMore == true) && flag == true){
			var len = $scope.products.length;
			var off = $scope.products[len-1].offset;
			if($scope.catType == 'popular'){
				flag = false;
				categoryService.getCategoryPopularFeed.get({id:catid , postType:"a", offset:off}, function(data){
					if (data.length == 0) {
						$scope.noMore = false;
					}
					angular.forEach(data, function(value, key) {
						if (!flag) {
							$scope.products.push(value);
						}
					});
					flag = true;
				});
			}

			if($scope.catType == 'newest'){
				flag = false;
				categoryService.getCategoryNewestFeed.get({id:catid , postType:"a", offset:off}, function(data){
					if (data.length == 0) {
						$scope.noMore = false;
					}
					angular.forEach(data, function(value, key) {
						if (!flag) {
							$scope.products.push(value);
						}
					});
					flag=true;
				});
			}
			
			if($scope.catType == 'high2low'){
				flag = false;
				categoryService.getCategoryPriceHighLowFeed.get({id:catid , postType:"a", offset:off}, function(data){
					if (data.length == 0) {
						$scope.noMore = false;
					}
					angular.forEach(data, function(value, key) {
						if (!flag) {
							$scope.products.push(value);
						}
					});
					flag=true;
				});
			}
			
			if($scope.catType == 'low2high'){
				flag = false;
				categoryService.getCategoryPriceLowHighFeed.get({id:catid , postType:"a", offset:off}, function(data){
					if (data.length == 0) {
						$scope.noMore = false;
					}
					angular.forEach(data, function(value, key) {
						if (!flag) {
							$scope.products.push(value);
						}
					});
					flag=true;
				});
			}
		}		
	}
	
	// UI helper
	$(window).scroll(function(e){
		$scope.position = window.pageYOffset;
		if($scope.position > 750) {
			$("#back-to-top").show();
		} else {
			$("#back-to-top").hide();
		}
	});
	
	$scope.gotoTop=function(){
		$location.hash('');
		$anchorScroll();
	};
});

babybox.controller('ProductPageController', 
		function($scope, $location, $translate, $route, $rootScope, $http, $window, likeService, userService, productService, product, userInfo, suggestedPost) {

	writeMetaCanonical($location.absUrl());
	/*
	writeMetaTitleDescription(
			product.title, 
			product.body, 
			formatToExternalUrl("/image/get-post-image-by-id/"+product.images[0]));
	*/
	
	$scope.product = product;
	$scope.userInfo = userInfo;
	$scope.suggestedPost = suggestedPost;

	$scope.like_Unlike = function(id) {
		if ($scope.userInfo.isLoggedIn) {
			if ($scope.userInfo.newUser) {
				$window.location.href ='/home';
			} else {
				if($scope.product.isLiked){
					likeService.unLikeProduct.get({id:id});
					$scope.product.isLiked = !$scope.product.isLiked;
					$scope.product.numLikes--;
				}else{
					likeService.likeProduct.get({id:id});
					$scope.product.isLiked = !$scope.product.isLiked;
					$scope.product.numLikes++;
				}
			}
		} else {
			$window.location.href ='/login';
		}
	}

	$scope.openPopup = function(){
		$('#popupBtn').click();
	}
	
});

babybox.controller('CommentOnProductController', 
		function($scope, $location, $translate, $route, $http, likeService) {

	writeMetaCanonical($location.absUrl());
	
	$scope.formData = {};
	$scope.comArray=[];
	$scope.submit = function() {
		var newCommentVM = {
				"postId" : $scope.product.id,
				"body" : $scope.formData.comment,
		};

		usSpinnerService.spin('loading...');
		$http.post('/comment/new', newCommentVM) 
		.success(function(response) {
			console.log(response);
			$scope.comArray.push({
				comment:$scope.formData.comment,
				name: $scope.userInfo.displayName,
				id: $scope.userInfo.id

			});
			$scope.formData.comment="";
			usSpinnerService.stop('loading...');
		});
	}
});


babybox.controller('ProfileController', 
		function($scope, $location, $translate, $route, $rootScope, $window, $anchorScroll, profileUser, userService, userInfo, followService, ngDialog) {

	writeMetaCanonical($location.absUrl());
	//writeMetaTitleDescription(profileUser.displayName, "看看 BabyBox 商店");

	$scope.toggleLang = function () {
		$translate.use() === 'zh'? $translate.use('en') : $translate.use('zh');
		location.reload();
	};
	 
	$scope.activeflag = true;
	$scope.userInfo = userInfo;
	$scope.user = profileUser;

	$scope.products = userService.getUserPostedFeed.get({id:profileUser.id, offset:0});
	
	$scope.onFollowUser = function() {
		if ($scope.userInfo.isLoggedIn) {
			if ($scope.userInfo.newUser) {
				$window.location.href ='/home';
			} else {
				followService.followUser.get({id:profileUser.id});
				$scope.user.isFollowing = !$scope.user.isFollowing;
				$scope.user.numFollowings++;
			}
		} else {
			$window.location.href ='/login';
		}
	}
	
	$scope.onUnFollowUser = function() {
		followService.unFollowUser.get({id:profileUser.id});
		$scope.user.isFollowing = !$scope.user.isFollowing;
		$scope.user.numFollowings--;
	}
	$scope.userProducts = function() {
		$scope.activeflag = true;
		$scope.noMore = true;
		$scope.products = userService.getUserPostedFeed.get({id:profileUser.id, offset:0});
	}
	$scope.likedProducts = function() {
		$scope.activeflag = false;
		$scope.noMore = true;
		$scope.products=userService.getUserLikedFeed.get({id:profileUser.id, offset:0});
	}
	
	var flag = true;
	$scope.noMore = true;
	
	$scope.loadMore = function () {	
		if(($scope.products.length!=0) && ($scope.noMore == true) && flag == true){
			var len = $scope.products.length;
			var off = $scope.products[len-1].offset;
			if($scope.activeflag){
				flag = false;
				userService.getUserPostedFeed.get({id:profileUser.id, offset:off}, function(data){
					if (data.length == 0) {
						$scope.noMore = false;
					}
					angular.forEach(data, function(value, key) {
						if (!flag) {
							$scope.products.push(value);
						}
					});
					flag=true;
				});
			}
			if(!$scope.activeflag){
				flag = false;
				userService.getUserLikedFeed.get({id:profileUser.id, offset:off}, function(data){
					if (data.length == 0) {
						$scope.noMore = false;
					}
					angular.forEach(data, function(value, key) {
						if (!flag) {
							$scope.products.push(value);
						}
					});
					flag=true;
				});
			}
		}		
	}
	
	// UI helper
	$(window).scroll(function(e){
		$scope.position = window.pageYOffset;
		if($scope.position > 750) {
			$("#back-to-top").show();
		} else {
			$("#back-to-top").hide();
		}
	});
	
	$scope.gotoTop=function(){
		$location.hash('');
		$anchorScroll();
	};
});

babybox.controller('CommentController', 
		function($scope, $location, $route, $translate, $http, $anchorScroll, comments, userInfo, productService) {
	
	writeMetaCanonical($location.absUrl());
	
	$scope.comments = comments;
	$scope.userInfo = userInfo;
	console.log($scope.comments);
	var url = $location.absUrl();
	var values= url.split("/");
	$scope.pid = values[values.length-1];
	$scope.submit = function(commentBody) {
		var newCommentVM = {
				"postId" : $scope.pid,
				"body" : commentBody,
		};
		$http.post('/comment/new', newCommentVM) 
		.success(function(response) {
			$scope.comments.push({
				body:commentBody,
				ownerName: $scope.userInfo.displayName,
				ownerId: $scope.userInfo.id
			});
			commentBody="";
		});
	}
	var off = 1;
	var flag = true;
	$scope.noMore = true;
	$scope.loadMore = function () {	
		if(($scope.comments.length!=0) && ($scope.noMore == true) && flag == true){
			flag = false;
			productService.allComments.get({id:$scope.pid, offset:off}, function(data){
				off++;
				if (data.length == 0) {
					$scope.noMore = false;
				}
				angular.forEach(data, function(value, key) {	
					if (!flag) {
						$scope.comments.push(value);
					}
				});
				flag=true;

			});
		}
	}

	// UI helper
	$(window).scroll(function(e){
		$scope.position = window.pageYOffset;
		if($scope.position > 750) {
			$("#back-to-top").show();
		} else {
			$("#back-to-top").hide();
		}
	});
	
	$scope.gotoTop=function(){
		$location.hash('');
		$anchorScroll();
	};
});

babybox.controller('UserFollowController', 
		function($scope, $translate, $location, $route, $anchorScroll, $http, followers, userInfo, followService) {
	
	writeMetaCanonical($location.absUrl());
	
	$scope.followers = followers;
	$scope.userInfo = userInfo;
	var url = $location.absUrl();
	var values= url.split("/");
	$scope.follow = values[values.length-2];
	if($scope.follow == 'followers')
		$scope.noMore = true;
	if($scope.follow == 'followings')
		$scope.noMore = true;
	
	$scope.onFollowUser = function(formFollower) {
		if(formFollower.id != $scope.userInfo.id){
			followService.followUser.get({id:formFollower.id});
			formFollower.isFollowing = !formFollower.isFollowing;
			formFollower.numFollowings++;
		}
	}
	$scope.onUnFollowUser = function(formFollower) {
		followService.unFollowUser.get({id:formFollower.id});
		formFollower.isFollowing = !formFollower.isFollowing;
		formFollower.numFollowings--;
	}

	var off = 1;
	var flag = true;
	$scope.noMore = true;
	$scope.loadMore = function () {	
		if(($scope.followers.length!=0) && ($scope.noMore == true) && flag == true){
			if($scope.follow == 'followers'){
				flag = false;
				followService.userfollowers.get({id:$scope.userInfo.id, offset:off}, function(data){
					off++;
					if (data.length == 0) {
						$scope.noMore = false;
					}
					angular.forEach(data, function(value, key) {
						if (!flag) {
							$scope.followers.push(value);
						}
					});
					flag=true;
				});
			}
			if($scope.follow == 'followings'){
				flag = false;
				followService.userfollowings.get({id:$scope.userInfo.id, offset:off}, function(data){
					console.log(data);
					console.log('followings');
					off++;
					if (data.length == 0) {
						$scope.noMore = false;
					}
					angular.forEach(data, function(value, key) {
						if (!flag) {
							$scope.followers.push(value);
						}
					});
					flag=true;
				});
			}	
		}	
	}
	
	// UI helper
	$(window).scroll(function(e){
		$scope.position = window.pageYOffset;
		if($scope.position > 750) {
			$("#back-to-top").show();
		} else {
			$("#back-to-top").hide();
		}
	});
	
	$scope.gotoTop=function(){
		$location.hash('');
		$anchorScroll();
	};
});

/*
 babybox.controller('CreateCollectionController', 
		function($scope, $location, $route, $http, usSpinnerService) {
	
	writeMetaCanonical($location.absUrl());
	
	$scope.formData = {};
	$scope.createCollection = function() {
		console.log($scope.formData);
		usSpinnerService.spin('loading...');
		$http.post('/create-collection', $scope.formData)
		.success(function(data){
			usSpinnerService.stop('loading...');
		});
	}
});
*/

babybox.controller('CreateProductController',
		function($scope, $location, $http, $upload, $validator, usSpinnerService, userInfo){
	
	writeMetaCanonical($location.absUrl());
	
	$scope.userInfo = userInfo;
	$scope.formData = {};
	$scope.selectedFiles =[];
	$scope.submitBtn = "發出";
	$scope.submit = function() {
		console.log($scope.formData);		
		var newPostVM = {
				"catId" : $scope.formData.category,
				"title" : $scope.formData.name,
				"body" : $scope.formData.body,
				"price" : $scope.formData.price,
		};
		console.log(newPostVM);

		usSpinnerService.spin('loading...');
		$validator.validate($scope, 'formData')
		.success(function () {
			usSpinnerService.spin('載入中...');
			$upload.upload({
				url: '/product/new/form',
				method: 'POST',
				file: $scope.selectedFiles,
				data: newPostVM,
				fileFormDataName: 'photo'
			}).progress(function(evt) {
				$scope.submitBtn = "請稍候...";
				usSpinnerService.stop('載入中...');
			}).success(function(data, status, headers, config) {
				$scope.submitBtn = "完成";
				usSpinnerService.stop('loading...');
			}).error(function(data, status, headers, config) {
				if( status == 505 ) {
					$scope.uniqueName = true;
					usSpinnerService.stop('載入中...');
					$scope.submitBtn = "再試一次";
				}  
			});
		})
		.error(function () {
			prompt("建立社群失敗。請重試");
		});
	}

	$scope.onFileSelect = function($files) {
		$scope.selectedFiles.push($files[0]);
		$scope.formData.photo = 'photo';
	}

});
