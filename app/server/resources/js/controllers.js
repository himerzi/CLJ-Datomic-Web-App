'use strict';

/* Controllers */
//$anchorScroll
dactic.
controller('SplashCtrl', ['$scope','$anchorScroll','$location', function($scope, $anchorScroll, $location){
	$anchorScroll();
	mixpanel.track("Splash");

}]).
controller('UserProfileCtrl', ["$scope", "ORM", '$routeParams', function($scope, orm, $routeParams){
	$scope.user = {name: "test"};
	if($routeParams.id){
		var user = orm.getUser($routeParams.id);
		$scope.user = user;
	}


}]).
controller('CourseListCtrl', ["$scope", "ORM", "$timeout", function($scope, orm, $timeout){
	var courses = orm.getCourseList();

	$scope.courses = courses
}]).
controller('CourseProposalCtrl', ['$scope', 'ORM', '$timeout', '$routeParams', function($scope, orm, $timeout, $routeParams){
	$scope.course = {takeaways: [], plan: [], title: "", "short-description": "", description: "", "about-instructor": "", state: ""};
	if($routeParams.id){
		$scope.course = orm.getCourseDetail($routeParams.id);
		console.log($scope.course)
	}
}]).
//nested inside course proposal controller in view
controller('CourseProposalFormCtrl', ['$scope', 'ORM', '$timeout', function($scope, orm, $timeout){
	mixpanel.track("Proposal submission page")
	$scope.master = {};
	$scope.form ={message: ""};

	$scope.saveProgress = function(form){

	};
	$scope.update = function(user) {
	  $scope.master = angular.copy(user);
	  console.log($scope.master)
	  orm.saveProposal($scope.master).then(function(response){
	  	mixpanel.track("Proposal Submitted", {"details": $scope.master});
	  	$scope["form"]['message'] = response;
	  	$timeout(function(){$scope["form"]['message'] = ""}, 3000);
	  },function(fail){
	  	$scope["form"]['message'] = "Error: " + fail[0];
	  	$timeout(function(){$scope["form"]['message'] = ""}, 3000);
	  })
	};
}]).
controller('CourseDetailCtrl', ['$scope', '$routeParams', "$location", 'ORM', '$sce', function($scope, $routeParams, $location, orm, $sce){
	$scope.resources = false;
	$scope.details = true;
	$scope.pay = ""
	if($routeParams.id){
		var path = $location.path().split("/");
		if($routeParams.id === "functional-programming-with-clojure"){
			$scope.pay = "https://spb.io/nZhL8iO5CC"
		}
		else if($routeParams.id === "functional-programming-with-clojure-mondays"){
			//tues
			$scope.pay = "https://spb.io/1mAQpDH7Tw"
		}
		else if($routeParams.id === "distilled-javascript---cooking-on-a-mean-stack"){
			$scope.pay = "https://spb.io/mZ3fAqgune"
		}
		else if($routeParams.id === "example-course"){
			$scope.pay = "example"
		}
		else{
			$scope.pay = "#"
		}
		if(path[path.length - 1] == "resources"){
			$scope.resources = true;
			$scope.details = false;
		}

		var id = $routeParams.id;
		mixpanel.track("Course Detail", {'Course': id});
		var course = orm.getCourseDetail(id);
		course['courseImg'] = "http://www.placekitten.com/200/312"
		$scope.course = course;

	}

}]).
controller("RegCtrl", ['$scope', "ORM", "$location", function($scope, orm, $location){
	$scope.master = {};
	$scope.login = {message:''}
	mixpanel.track("Registration page");
	$scope.update = function(user) {
	  $scope.master = angular.copy(user);
	  console.log($scope.master)
	  orm.registerUser($scope.master).then(function(resp){
	  	$scope.showSuccess = true;
	  	$scope.logMeIn(function(){
	  		$location.path('/profile/' + $scope.currentUser.id);
	  	});
	  },function(fail){
	  	$scope["login"]['message'] = fail[0];
	  })
	};
}]).
controller('CourseResourceCtrl', ['$scope', 'ORM', '$timeout', function($scope, orm, $timeout){
	$scope.master = {};

	$scope.removeResource = function(rid, index){
		console.log(rid + " " + index)
		mixpanel.track("Remove Resource");
		orm.removeCourseResource(rid, $scope.course.slug, $scope.course.eid).then(function(){
			$scope.course["resources"].splice(index, 1);
		})
	}
	$scope.update = function(user) {
		mixpanel.track("Add Resource");
	 $scope.course.resources.push({ "description": user.description, "url": user.url});
	  $scope.master = angular.copy(user);
	  $scope.master["cid"] = $scope.course.eid;
	  $scope.master["slug"] = $scope.course.slug;
	  console.log($scope.master)
	  orm.saveCourseResource($scope.master).then(function(){
	  })
	};
}]).
controller('StartupFormRegCtrl', ['$scope', 'ORM', '$timeout', function($scope, orm, $timeout){
	$scope.master = {};

	$scope.update = function(user) {
	  $scope.master = angular.copy(user);
	  console.log($scope.master)

	  orm.registerStartup($scope.master).then(function(){
	  	mixpanel.track("Startup Registered", {"details": $scope.master});
	  	$scope.showSuccess = true;
	  	$timeout(function(){$scope.showSuccess = false}, 3000);
	  });

	};

	$scope.reset = function() {
	  $scope.company = angular.copy($scope.master);
	};

	$scope.reset();

}]).
controller('UpdatesFormRegCtrl', ['$scope', 'ORM', '$timeout', function($scope, orm, $timeout){
	$scope.master = {};
	$scope.master.formPlaceholder = "your@email.com"
	$scope.master.button = "Get Updates"

	$scope.update = function(user) {
	  $scope.master = angular.copy(user);
	  console.log($scope.master)
	  orm.regForEmail($scope.master).then(function(){
	  	$scope.master.button = "Thanks!"
	  });

	};

}]).
controller('LogoutCtrl', ['Session', function(session){
}]).
controller('LoginFormCtrl', ['$scope', 'ORM', 'Session', '$timeout', '$location', function($scope, orm, session, $timeout, $location){
	$scope.login = {};

	$scope.update = function(user) {
	  $scope.login = angular.copy(user) || {};

	  session.logIn($scope.login ).then(function(resp){
	  	$scope.setLoggedIn($scope.login.email, resp["profile-location"]);
	  	$location.path('/profile/' + resp["profile-location"]);
	  }, function(fail){
	  	$scope.login['message'] = fail[0];
	  });

	};

}]).
controller('StudentCtrl', ["$anchorScroll", function($anchorScroll){
	mixpanel.track("Student Page");
	//$anchorScroll();
}]).
controller('TeachingCtrl', [function(){
	mixpanel.track("Teaching Page");
}]).
controller('StartupsCtrl', ['$scope','$anchorScroll','$location', function($scope, $anchorScroll, $location){
	mixpanel.track("Startup Page");

}]).
controller('MainCtrl', ['$scope','$anchorScroll','$location', 'ORM', "Session", function($scope, $anchorScroll, $location, orm, session){
	$scope.currentUser = {'state': false, 'email': '', "id": ''};
	var init = function(){
	  // $scope.currentUser['state'] = (Parse.User.current())? true : false;
	  $scope.logMeIn();
	}


	$scope.logMeIn = function(cb){
		session.isLoggedIn().then(function(data){
			if(data.loggedIn){
				$scope.setLoggedIn(data.id, data.profile);
				if (cb) {cb()};
			}
		})
	}
	init();
	$scope.logOut = function(){
		session.logOut().then(function(){
			$scope.setLoggedOut();
		});
	}
	$scope.setLoggedOut = function(username){
      $scope.currentUser.email = "";
      $scope.currentUser.state = false;
    }
    $scope.setLoggedIn = function(username, id){
      $scope.currentUser.email = username;
      $scope.currentUser.state = true;
      $scope.currentUser.id = id;
    }
	$scope.goTo = function(where) {
		// set the location.hash to the id of
		// the element you wish to scroll to.
		$location.hash(where);

		// call $anchorScroll()
		$anchorScroll();
	}

}]);