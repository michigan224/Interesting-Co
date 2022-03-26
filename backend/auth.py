import jwt

BACKEND_SECRET = "interesting_co_rememri_secret"

# Checks if valid token in Authorization header
# Authorization header should be in format "Bearer eyJ0eXAiOiJKV1QiLCJ..."
def auth(request, requesting_username):
    header = request.headers.get('Authorization')
    if not header:
        return False

    try:
        token = header.split('Bearer')[1].strip()
        token_user = jwt.decode(token, BACKEND_SECRET, algorithms=["HS256"])
        if token_user['username'] != requesting_username:
            return False

        return True
    except Exception as e:
        print("exception", e)
        return False

def generate_user_token(user):
    token = jwt.encode(user, BACKEND_SECRET, algorithm="HS256")
    return token