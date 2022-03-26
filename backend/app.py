# app.py

# Required imports
import json
import os
from flask import Flask, request, jsonify
from firebase_admin import credentials, firestore, initialize_app
from geopy import distance
import hashlib
import uuid
from auth import auth, generate_user_token

# Initialize Flask app
app = Flask(__name__)

# Initialize Firestore DB
cred = credentials.Certificate('key.json')
default_app = initialize_app(cred)
db = firestore.client()
pins_ref = db.collection('pins')
users_ref = db.collection('users')


@app.route('/accounts/sign_up', methods=['POST'])
def sign_up():
    try:
        username = request.json['username']
        password = request.json['password']

        # users = get_users()
        # for user in users:
        #     if user['username'] == username:
        #         return jsonify({
        #             'message': "Username already exists."
        #         }), 409
        user = users_ref.where('username', '==', username).get()
        if bool(user):
            return jsonify({
                'message': "Username already exists."
            }), 409

        algorithm = 'sha512'
        salt = uuid.uuid4().hex
        hash_obj = hashlib.new(algorithm)
        password_salted = salt + password
        hash_obj.update(password_salted.encode('utf-8'))
        password_hash = hash_obj.hexdigest()
        hashed_password = "$".join([algorithm, salt, password_hash])

        token = generate_user_token({'username': username}).decode('utf-8')

        data = {
            "username": username,
            "password": hashed_password,
            "token": token
        }

        users_ref.add(data)[1]

        return jsonify({
            'token': token
        }), 201

    except Exception as e:
        return f"An Error Occured: {e}", 400


@app.route('/accounts/sign_in', methods=['POST'])
def sign_in():
    try:
        username = request.json['username']
        password = request.json['password']

        user = users_ref.where('username', '==', username).get()
        if bool(user):
            salt = user[0].to_dict()['password'].split('$')[1]
            algorithm = 'sha512'
            hash_obj = hashlib.new(algorithm)
            password_salted = salt + password
            hash_obj.update(password_salted.encode('utf-8'))
            password_hash = hash_obj.hexdigest()
            hashed_password = "$".join([algorithm, salt, password_hash])
            if user[0].to_dict()['password'] == hashed_password:
                return jsonify({
                    'message': 'success',
                    'token': user[0].to_dict()['token']
                }), 200
            else:
                # Invalid password
                return jsonify({
                    'message': 'Invalid credentials.'
                }), 401
        else:
            # User not found
            return jsonify({
                'message': 'Invalid credentials.'
            }), 401

    except Exception as e:
        return f"An Error Occured: {e}", 400


@app.route('/friends', methods=['GET'])
def get_friends():
    try:
        username = request.args.get('username')
        if auth(request, username) is False:
            return jsonify({
                'message': 'Unauthorized user.'
            }), 403
        users = get_users()
        for user in users:
            if user['username'] == username:
                friends = user['friends'] if 'friends' in user else []
                return jsonify(friends), 200

        return jsonify({
            'message': 'No user found.'
        }), 401

    except Exception as e:
        return f"An Error Occured: {e}", 400

# Handles accepting, rejecting and making friend requests


@app.route('/friend_request', methods=['POST'])
def friend_request():
    # Checks if user1 and user2 are already friends
    def check_already_friends(user1, user2):
        user_doc = users_ref.where('username', '==', user1).get()[0].reference

        # Check if requester is already friends with requestee
        user_friends = user_doc.collection('friends')
        user2_is_friend = user_friends.where('username', '==', user2).get()
        return bool(user2_is_friend)

    # Adds requestee to list of requester's friends and adds requester to list of requestee's friends
    def add_friend(requestee, requester):
        # Check if requester is already friends with requestee
        if check_already_friends(requestee, requester):
            return jsonify({
                'message': 'Users are already friends.'
            }), 400

        users_ref.where('username', '==', requestee).get()[0].reference.collection('friends').add({
            "username": requester
        })
        users_ref.where('username', '==', requester).get()[0].reference.collection('friends').add({
            "username": requestee
        })

        return jsonify({
            'message': 'Success'
        }), 200

    # Removes requestee from requester's outgoing requests and removes requester from requestee's incoming requests
    def remove_incoming_and_outgoing_requests(requestee, requester):
        try:
            outgoing_requests = users_ref.where('username', '==', requester).get()[
                0].reference.collection('outgoing_requests')
            outgoing_request = outgoing_requests.where(
                'username', '==', requestee).get()[0].reference
            outgoing_request.delete()

            incoming_requests = users_ref.where('username', '==', requestee).get()[
                0].reference.collection('incoming_requests')
            incoming_request = incoming_requests.where(
                'username', '==', requester).get()[0].reference
            incoming_request.delete()

            return True
        except IndexError:
            return False

    # Adds requestee to requester's outgoing requests and adds requester to requestee's incoming requests
    def make_request(requester, requestee):
        user_doc = users_ref.where('username', '==', requester).get()[
            0].reference

        # First check for logical consistency errors

        # Check if requester is already friends with requestee
        if check_already_friends(requestee, requester):
            return jsonify({
                'message': 'Users are already friends.'
            }), 400

        # Check if requester already made friend request to requestee
        outgoing_requests = user_doc.collection('outgoing_requests')
        requester_already_requested_doc = outgoing_requests.where(
            'username', '==', requestee).get()
        if bool(requester_already_requested_doc):
            return jsonify({
                'message': 'Target user has already been friend requested.'
            }), 400

        # Check if requestee already made friend request to requester
        incoming_requests = user_doc.collection('incoming_requests')
        requestee_already_requested_doc = incoming_requests.where(
            'username', '==', requestee).get()
        if bool(requestee_already_requested_doc):
            return jsonify({
                'message': 'Current user has already been friend requested by target user.'
            }), 400

        # Execute friend request
        requestee_doc = users_ref.where(
            'username', '==', requestee).get()[0].reference
        requestee_incoming_requests = requestee_doc.collection(
            'incoming_requests')

        outgoing_requests.add({
            "username": requestee
        })
        requestee_incoming_requests.add({
            "username": requester
        })

        return jsonify({
            'message': 'Success'
        }), 200

    try:
        current_username = request.json['user1']
        if auth(request, current_username) is False:
            return jsonify({
                'message': 'Unauthorized user.'
            }), 403

        target_username = request.json['user2']
        action = request.json['action']

        target_user = users_ref.where('username', '==', target_username).get()
        if bool(target_user) is False:
            return jsonify({
                'message': "Target user does not exist."
            }), 409

        if action == "request":
            return make_request(current_username, target_username)
        elif action == "accept":
            if remove_incoming_and_outgoing_requests(current_username, target_username) is False:
                return jsonify({
                    'message': 'Friend request not found.'
                }), 400
            return add_friend(current_username, target_username)
        elif action == "reject":
            if remove_incoming_and_outgoing_requests(current_username, target_username) is False:
                return jsonify({
                    'message': 'Friend request not found.'
                }), 400
            return jsonify({
                'message': 'Success'
            }), 200
        else:
            return jsonify({
                'message': 'Invalid action: ' + action
            }), 400

    except Exception as e:
        return f"An Error Occured: {e}", 400


@app.route('/friend_requests/incoming', methods=['GET'])
def get_incoming_requests():
    try:
        username = request.args.get('username')
        if auth(request, username) is False:
            return jsonify({
                'message': 'Unauthorized user.'
            }), 403
        users = get_users()
        for user in users:
            if user['username'] == username:
                incoming_requests = user['incoming_requests'] if 'incoming_requests' in user else [
                ]
                return jsonify(incoming_requests), 200

        return jsonify({
            'message': 'No user found.'
        }), 401
    except Exception as e:
        return f"An Error Occured: {e}", 400


@app.route('/friend_requests/outgoing', methods=['GET'])
def get_outgoing_requests():
    try:
        username = request.args.get('username')
        if auth(request, username) is False:
            return jsonify({
                'message': 'Unauthorized user.'
            }), 403
        users = get_users()
        for user in users:
            if user['username'] == username:
                outgoing_requests = user['outgoing_requests'] if 'outgoing_requests' in user else [
                ]
                return jsonify(outgoing_requests), 200

        return jsonify({
            'message': 'No user found.'
        }), 401
    except Exception as e:
        return f"An Error Occured: {e}", 400

# GET /nearby_pins


@app.route('/nearby_pins', methods=['GET'])
def nearby_pins():
    """Returns a list of nearby pins"""
    PIN_RADIUS = request.args.get('radius') or 15
    try:
        username = request.args.get('username')
        if username:
            if auth(request, username) is False:
                return jsonify({
                    'message': 'Unauthorized user.'
                }), 403
            current_location = request.args.get('current_location')
            if not current_location:
                return jsonify({
                    'message': 'No location provided.'
                }), 400
            current_location = current_location.split(',')
            pins = get_pins()
            user = users_ref.where('username', '==', username).get()
            if not user:
                return jsonify({"message": "User not found"}), 401
            visible_pins = [
                pin for pin in pins if (pin['is_public'] or pin['owner_id'] in user['friends'])]
            for pin in visible_pins:
                print(distance.distance(
                    tuple(pin['location']), tuple(current_location)).miles)
            nearby_pins = [pin for pin in visible_pins if distance.distance(
                tuple(pin['location']), tuple(current_location)).miles < PIN_RADIUS]
            return jsonify(nearby_pins), 200
        else:
            pins = get_pins()
            visible_pins = [
                pin for pin in pins if pin['is_public']]
            current_location = request.args.get('current_location')
            if not current_location:
                return jsonify({
                    'message': 'No location provided.'
                }), 400
            current_location = current_location.split(',')
            nearby_pins = [pin for pin in visible_pins if distance.distance(
                tuple(pin['location']), tuple(current_location)).miles < PIN_RADIUS]
            return jsonify(nearby_pins), 200
    except Exception as e:
        return f"An Error Occured: {e}"


@app.route('/pin/<pin_id>', methods=['GET'])
def specific_pin(pin_id):
    """Returns a specific pin if it is accessible by the user"""
    try:
        username = request.args.get('username')
        if username:
            if auth(request, username) is False:
                return jsonify({
                    'message': 'Unauthorized user.'
                }), 403
            user = users_ref.where('username', '==', username).get()
            if not user:
                return jsonify({"message": "User not found"}), 401
            ret_pin = pins_ref.where('post_id', '==', pin_id).get()
            if not ret_pin:
                return jsonify({"error": "Pin not found, or not accessible by user"}), 404
            return jsonify(ret_pin), 200
        else:
            pin = pins_ref.where('post_id', '==', pin_id).get()
            if not pin:
                return jsonify({"error": "Pin not found"}), 404
            elif not pin["is_public"]:
                return jsonify({"error": "Pin not accessible by user"}), 404
            return jsonify(pin), 200

    except Exception as e:
        return f"An Error Occured: {e}"


@ app.route('/pin', methods=['POST'])
def post_pin():
    """Creates a new pin"""
    try:
        username = request.json['username']
        user = users_ref.where('username', '==', username).get()
        if not user:
            return jsonify({
                'message': 'User not found'
            }), 404
        elif not auth(request, username):
            return jsonify({
                'message': 'Unauthorized user.'
            }), 403

        is_public = request.json['is_public']
        pin_location = request.json['pin_location']
        image = request.json['image']
        post_id = request.json['post_id']
        data = {
            'is_public': is_public,
            'location': pin_location,
            'owner_id': username,
            'timestamp': firestore.SERVER_TIMESTAMP,
            'media_url': image,
            'post_id': post_id
        }
        new_pin = pins_ref.add(data)[1]
        new_pin.collection('comments').add({})
        return jsonify({"success": True}), 200
    except Exception as e:
        return f"An Error Occured: {e}"


@ app.route('/comment', methods=['POST'])
def post_comment():
    """Creates a new comment"""
    try:
        username = request.json['username']
        if not username:
            return jsonify({
                'message': 'No username provided.'
            }), 400
        user = users_ref.where('username', '==', username).get()
        if not user:
            return jsonify({
                'message': 'User not found'
            }), 404
        elif not auth(request, username):
            return jsonify({
                'message': 'Unauthorized user.'
            }), 403
        text = request.json['comment_text']
        if not text:
            return jsonify({
                'message': 'No comment provided.'
            }), 400
        post_id = request.json['post_id']
        if not post_id:
            return jsonify({
                'message': 'No post_id provided.'
            }), 400
        data = {
            'owner_id': username,
            'text': text,
            'timestamp': firestore.SERVER_TIMESTAMP,
        }
        pin = pins_ref.where(
            'post_id', '==', post_id).get()
        if not pin:
            return jsonify({"error": "Pin not found"}), 404
        pin_doc = pins_ref.document(pin[0].id)
        pin_doc.collection('comments').add(data)
        return jsonify({"success": True}), 200
    except Exception as e:
        return f"An Error Occured: {e}"


def get_pins():
    pins = []
    for doc in pins_ref.stream():
        pin = doc.to_dict()
        for collection_ref in doc.reference.collections():
            vals = [vals.to_dict()
                    for vals in collection_ref.stream() if bool(vals.to_dict())]
            if vals:
                pin[collection_ref.id] = vals
            else:
                pin[collection_ref.id] = []
        pins.append(pin)
    return pins


def get_users():
    users = []
    for doc in users_ref.stream():
        user = doc.to_dict()
        for collection_ref in doc.reference.collections():
            print(collection_ref.id)
            vals = [val.to_dict()['username']
                    for val in collection_ref.stream()]
            user[collection_ref.id] = vals
        users.append(user)
    return users

# Old boilerplate code for reference

# todo_ref = db.collection('todos')

# @app.route('/add', methods=['POST'])
# def create():
#     """
#         create() : Add document to Firestore collection with request body.
#         Ensure you pass a custom ID as part of json body in post request,
#         e.g. json={'id': '1', 'title': 'Write a blog post'}
#     """
#     try:
#         id = request.json['id']
#         todo_ref.document(id).set(request.json)
#         return jsonify({"success": True}), 200
#     except Exception as e:
#         return f"An Error Occured: {e}"


# @app.route('/list', methods=['GET'])
# def read():
#     """
#         read() : Fetches documents from Firestore collection as JSON.
#         todo : Return document that matches query ID.
#         all_todos : Return all documents.
#     """
#     try:
#         # Check if ID was passed to URL query
#         todo_id = request.args.get('id')
#         if todo_id:
#             todo = todo_ref.document(todo_id).get()
#             return jsonify(todo.to_dict()), 200
#         else:
#             all_todos = [doc.to_dict() for doc in todo_ref.stream()]
#             return jsonify(all_todos), 200
#     except Exception as e:
#         return f"An Error Occured: {e}"


# @app.route('/update', methods=['POST', 'PUT'])
# def update():
#     """
#         update() : Update document in Firestore collection with request body.
#         Ensure you pass a custom ID as part of json body in post request,
#         e.g. json={'id': '1', 'title': 'Write a blog post today'}
#     """
#     try:
#         id = request.json['id']
#         todo_ref.document(id).update(request.json)
#         return jsonify({"success": True}), 200
#     except Exception as e:
#         return f"An Error Occured: {e}"


# @app.route('/delete', methods=['GET', 'DELETE'])
# def delete():
#     """
#         delete() : Delete a document from Firestore collection.
#     """
#     try:
#         # Check for ID in URL query
#         todo_id = request.args.get('id')
#         todo_ref.document(todo_id).delete()
#         return jsonify({"success": True}), 200
#     except Exception as e:
#         return f"An Error Occured: {e}"


port = int(os.environ.get('PORT', 8080))
if __name__ == '__main__':
    app.run(threaded=True, host='0.0.0.0', port=port)
