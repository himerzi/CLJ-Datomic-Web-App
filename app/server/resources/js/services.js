'use strict';

/* Services */

dactic.
    provider('_Parse', function(){
    		Parse.initialize("");
  		this.$get = function(){
  			return Parse || {}
  		}

	}).
	service("Utils", [function(){
		var _listifyErrors = function(response){
			var errList = [];
			var data = response.data.errors;
			for (var key in data) {
			   if (data.hasOwnProperty(key)) {
			   	data[key].forEach(function(element, index, array){
			   		errList.push(element);
			   	})
			   }
			}
			return errList;
		}

		this.listifyErrors = _listifyErrors;
	}]).
    service('Session', ["$resource","$q","Utils", function($resource, $q, utils){
    	var User = $resource('/user');
    	var Login = $resource('/login');
    	var Logout = $resource('/logout');
    	//I believe services are singletons, so this should only run once.

    	var _logOut = function(){
    		var deferred = $q.defer();
    		Logout.delete(function(){
    			deferred.resolve();
    		})
    		return deferred.promise;
    	}
    	this.logOut = _logOut

    	var _logIn = function(loginDeets){
    		var email = loginDeets.email || '';
    		var password = loginDeets.password || '';
    		var deferred = $q.defer();
    		Login.save({password: password, email: email} ,function(success){
    			mixpanel.track("Succesful Registration");
    			mixpanel.identify(email);
    			deferred.resolve(success);
    		}, function(fail){
    			deferred.reject(utils.listifyErrors(fail));
    			mixpanel.track("Site Error", {'error': fail});
    		})
    		return deferred.promise;
    	}
    	this.logIn = _logIn

    	var _isLoggedIn = function(){
    		var deferred = $q.defer();
			User.get(function(resp){
				deferred.resolve(resp);
			})
			return deferred.promise
    	}
    	this.isLoggedIn = _isLoggedIn;

    	var _getEmail = function(){
    		var deferred = $q.defer();
			User.get(function(resp){
				if(resp.loggedIn){
					deferred.resolve(resp.id)
				}
				deferred.reject("not logged in");
			})
			return deferred.promise
    	}
    }]).
	service('DacticParse', ["_Parse", '$q', function(_Parse, $q){
		var _get = function(className, id){
			var deferred = $q.defer();

			var objClass = Parse.Object.extend(className);
			var query = new Parse.Query(objClass);
			query.get(id, {
			  success: function(obj) {
			    deferred.resolve(obj);
			  },
			  error: function(object, error) {
			    mixpanel.track("Parse Error", {'code': error.code});
			    deferred.reject(error);
			  }
			});

			return deferred.promise;
		}

		this.getParseObj = _get;

		var _getCourseBySlug = function(id){
			var deferred = $q.defer();

			var objClass = Parse.Object.extend('Course');
			var query = new Parse.Query(objClass);
			query.limit(1);
			query.equalTo("slug", id);
			query.find({
			  success: function(obj) {
			    deferred.resolve(obj);
			  },
			  error: function(object, error) {
			    mixpanel.track("Parse Error", {'code': error.code});
			    deferred.reject(error);
			  }
			});

			return deferred.promise;
		}

		this.getCourseBySlug = _getCourseBySlug;

	}]).
	service('ORM', ["_Parse", "DacticParse", '$q', "$resource", "Utils", function(_Parse, DacticParse, $q, $resource, utils){

	    /* Public methods */

	    var _registerUser = function(regDeets){
	     var deferred = $q.defer();
        var Register = $resource('/register');

        var prom = Register.save({name: regDeets.name, password: regDeets.password, email: regDeets.email},
         function(something){
         	mixpanel.people.set({ $email: regDeets.email, $name: regDeets.name});
         	mixpanel.identify(regDeets.email);
             deferred.resolve(something);
         },
         function(fail){
         	deferred.reject(utils.listifyErrors(fail));
         });

        return deferred.promise;
	    }
	    this.registerUser = _registerUser;

	    var _User = function(id){

	    	var User = $resource('/users/:id');
	    	return User.get({id: id});
	    }
	    this.getUser = _User;

	    var _registerStartup = function (regDeets) {
	      var deferred = $q.defer();
	      // if(!('email' in regDeets && 'postCode' in regDeets && 'companyName' in regDeets) ){
	      // 	throw {"message": "Invalid startup registration arguments"}
	      // }
	      regDeets.email = regDeets.email || ''
	      regDeets.postCode = regDeets.postCode || ''
	      regDeets.companyName = regDeets.companyName || ''

	      var startupRegEntry = _Parse.Object.extend("StartupRegEntry");
	      var newEntry = new startupRegEntry();

	      newEntry.save({
	      	email:  regDeets.email,
	      	postCode:  regDeets.postCode,
	      	companyName: regDeets.companyName
	      }, {
	        success: function(entrySaved) {
	          console.log('Saved new entry with objectId: ' + entrySaved.id);
	          deferred.resolve();
	        },
	        error: function(entry, error) {
	          // Execute any logic that should take place if the save fails.
	          // error is a Parse.Error with an error code and description.
	          console.log('Failed to create new object, with error code: ' + error.description);
	        }
	      });
	      return deferred.promise;

	    }

	    this.registerStartup = _registerStartup;

	    var _regForEmail = function (regDeets) {
	      var deferred = $q.defer();
	      // if(!('email' in regDeets && 'postCode' in regDeets && 'companyName' in regDeets) ){
	      // 	throw {"message": "Invalid startup registration arguments"}
	      // }
	      regDeets.email = regDeets.email || ''

	      var updateRegEntry = _Parse.Object.extend("updateRegEntry");
	      var newEntry = new updateRegEntry();

	      newEntry.save({
	      	email:  regDeets.email
	      }, {
	        success: function(entrySaved) {
	          console.log('Saved new entry with objectId: ' + entrySaved.id);
	          deferred.resolve();
	        },
	        error: function(entry, error) {
	          // Execute any logic that should take place if the save fails.
	          // error is a Parse.Error with an error code and description.
	          console.log('Failed to create new object, with error code: ' + error.description);
	        }
	      });
	      return deferred.promise;

	    }

	    this.regForEmail = _regForEmail;

	    var _getCourseList = function(){
	    	var deferred = $q.defer();
	        var Course = $resource('/courses');

	        return Course.query();
	    }

	    this.getCourseList = _getCourseList;

	    var _getCourseDetail = function (id) {
	        var deferred = $q.defer();
	        var Course = $resource('/courses/:id');

	        return Course.get({id: id});

	    }

	    this.getCourseDetail = _getCourseDetail;

	    var _saveProposal = function (proposal) {
	        var deferred = $q.defer();
	        var courseProposal = $resource('/courses');
	        var newEntry = {
	  	    "instructor-about":  proposal['instructor-about'],
	  	    plan: proposal.plan,
	  	    "short-description": proposal['short-description'],
	  	    takeaways: proposal.takeaways,
	  	    description: proposal.description,
	  	    title: proposal.title,
	  	    state: proposal.state,
	  	    "programming-environment": proposal["programming-environment"],
	        };

                var coursePromise = courseProposal.save(newEntry,
                    function(response){
                        deferred.resolve(response['course-message']);},
                    function(fail){
                    	deferred.reject(utils.listifyErrors(fail));
                    	mixpanel.track("Site Error", {'error': fail});                                  });

	        return deferred.promise;

	    }

	    this.saveProposal = _saveProposal;

	    var _saveCourseResource = function (resource) {
	        var deferred = $q.defer();
	        var courseProposal = $resource('/courses/:cid/resources',{cid:'@slug'});
	        var newEntry = {
	        cid: resource.cid,
	        slug: resource.slug,
	  	    url:  resource.url,
	  	    description:  resource.description
	        };

            var coursePromise = courseProposal.save(newEntry,
                function(something){
                    console.log(something);},
                function(fail){
                	mixpanel.track("Site Error", {'error': fail});
                              });

	        return deferred.promise;

	    }

	    this.saveCourseResource = _saveCourseResource;

	    var _removeCourseResource = function (rid, slug, cid) {
	        var deferred = $q.defer();
	        var CourseResource = $resource('/courses/:slug/resources',{slug:'@slug'});

            var CoursePromise = CourseResource.delete({slug: slug, cid: cid, eid: rid},
                function(something){
                    deferred.resolve(something);
                },
                function(fail){
                	mixpanel.track("Site Error", {'error': fail});
                });

	        return deferred.promise;

	    }

	    this.removeCourseResource = _removeCourseResource;
  }]);
