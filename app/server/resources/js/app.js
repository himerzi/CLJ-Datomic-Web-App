'use strict';

/* App Module */

var dactic = angular.module('dactic', ['ngRoute', 'ngResource'])

dactic.config(['$routeProvider', function($routeProvider) {
  $routeProvider.
      when('/startups', {templateUrl: 'partials/startups.html', controller: 'StartupsCtrl'}).
      when('/students', {templateUrl: 'partials/student-faqs.html', controller: 'StudentCtrl'}).
      when('/profile/:id', {templateUrl: 'partials/user-detail.html', controller: ''}).
      when('/courses/cc', {templateUrl: 'partials/cc-detail.html', controller: ''}).
      when('/courses/thank-you', {templateUrl: 'partials/sicp-post-reg.html', controller: ''}).
      when('/courses/:id/resources', {templateUrl: 'partials/course-detail/course-header.html', controller: 'CourseDetailCtrl'}).
      when('/courses/:id', {templateUrl: 'partials/course-detail/course-header.html', controller: 'CourseDetailCtrl'}).
      when('/courses/sicp', {templateUrl: 'partials/sicp-detail.html', controller: 'CourseDetailCtrl'}).
      when('/teaching', {templateUrl: 'partials/teaching-detail.html', controller: 'TeachingCtrl'}).
      when('/teaching/submit-proposal', {templateUrl: 'partials/course-proposal.html', controller: 'CourseProposalCtrl'}).
      when('/teaching/new', {templateUrl: 'partials/base-course-form.html'}).
      when('/teaching/course/:id', {templateUrl: 'partials/base-course-form.html'}).
      when('/login', {templateUrl: 'partials/login.html', controller: 'LoginFormCtrl'}).
      when('/logout', {controller: 'LogoutCtrl'}).
      when('/register', {templateUrl: 'partials/register.html', controller: 'RegCtrl'}).
      when('/', {templateUrl: 'partials/index.html', controller: 'SplashCtrl'}).
      otherwise({redirectTo: '/'});
}]).
run(["$rootScope", "$location", "$anchorScroll", "$routeParams", "$timeout", function($rootScope, $location, $anchorScroll, $routeParams, $timeout) {
  $rootScope.$on('$routeChangeSuccess', function(newRoute, oldRoute) {
    //$location.hash($routeParams.scrollTo);
    $timeout($anchorScroll, 500);
  });
}]);
