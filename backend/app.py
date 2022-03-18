import os

from firebase_admin import credentials, firestore, initialize_app
from flask import Flask, jsonify, request
from geopy import distance

app = Flask(__name__)

cred = credentials.Certificate('key.json')
default_app = initialize_app(cred)
db = firestore.client()
pins_ref = db.collection('pins')
users_ref = db.collection('users')


# GET /nearby_pins
@app.route('/nearby_pins', methods=['GET'])
def nearby_pins():
    """Returns a list of nearby pins"""
    PIN_RADIUS = 15  # miles
    try:
        username = request.json['username']
        current_location = request.json['current_location']
        pins = get_pins()
        users = get_users()
        user = next(
            (item for item in users if item['username'] == username), None)
        visible_pins = [
            pin for pin in pins if (pin['is_public'] or pin['owner_id'] in user['friends'])]
        for pin in visible_pins:
            print(distance.distance(
                tuple(pin['location']), tuple(current_location)).miles)
        nearby_pins = [pin for pin in visible_pins if distance.distance(
            tuple(pin['location']), tuple(current_location)).miles < PIN_RADIUS]
        return jsonify(nearby_pins), 200
    except Exception as e:
        return f"An Error Occured: {e}"

# GET /pin/<pin_id>


@app.route('/pin/<pin_id>', methods=['GET'])
def specific_pin(pin_id):
    """Returns a specific pin if it is accessible by the user"""
    try:
        username = request.json['username']
        pins = get_pins()
        users = get_users()
        user = next(
            (item for item in users if item['username'] == username), None)
        visible_pins = [
            pin for pin in pins if (pin['is_public'] or pin['owner_id'] in user['friends'])]
        ret_pin = None
        for pin in visible_pins:
            if pin['post_id'] == pin_id:
                ret_pin = pin
        if not ret_pin:
            return jsonify({"error": "Pin not found, or not accessible by user"}), 404
        return jsonify(ret_pin), 200
    except Exception as e:
        return f"An Error Occured: {e}"


@ app.route('/pin', methods=['POST'])
def post_pin():
    """Creates a new pin"""
    try:
        username = request.json['username']
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
        print(new_pin)
        new_pin.collection('comments').add({})
        return jsonify({"success": True}), 200
    except Exception as e:
        return f"An Error Occured: {e}"


@ app.route('/comment', methods=['POST'])
def post_comment():
    """Creates a new comment"""
    try:
        username = request.json['username']
        text = request.json['comment_text']
        post_id = request.json['post_id']
        data = {
            'owner_id': username,
            'text': text,
            'timestamp': firestore.SERVER_TIMESTAMP,
        }
        for doc in pins_ref.stream():
            if 'post_id' in doc.to_dict() and doc.to_dict()['post_id'] == post_id:
                for collection_ref in doc.reference.collections():
                    print(collection_ref.id)
                    if collection_ref.id == 'comments':
                        collection_ref.add(data)
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


@ app.route('/add', methods=['POST'])
def create():
    """
        create() : Add document to Firestore collection with request body.
        Ensure you pass a custom ID as part of json body in post request,
        e.g. json={'id': '1', 'title': 'Write a blog post'}
    """
    try:
        id = request.json['id']
        todo_ref.document(id).set(request.json)
        return jsonify({"success": True}), 200
    except Exception as e:
        return f"An Error Occured: {e}"


@ app.route('/update', methods=['POST', 'PUT'])
def update():
    """
        update() : Update document in Firestore collection with request body.
        Ensure you pass a custom ID as part of json body in post request,
        e.g. json={'id': '1', 'title': 'Write a blog post today'}
    """
    try:
        id = request.json['id']
        todo_ref.document(id).update(request.json)
        return jsonify({"success": True}), 200
    except Exception as e:
        return f"An Error Occured: {e}"


@ app.route('/delete', methods=['GET', 'DELETE'])
def delete():
    """
        delete() : Delete a document from Firestore collection.
    """
    try:
        # Check for ID in URL query
        todo_id = request.args.get('id')
        todo_ref.document(todo_id).delete()
        return jsonify({"success": True}), 200
    except Exception as e:
        return f"An Error Occured: {e}"


port = int(os.environ.get('PORT', 8080))
if __name__ == '__main__':
    app.run(threaded=True, host='0.0.0.0', port=port)
