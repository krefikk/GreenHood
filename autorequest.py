import os
from supabase import create_client, Client

url = os.environ.get("SUPABASE_URL")
key = os.environ.get("SUPABASE_KEY")

def main():
    if not url or not key:
        print("Error: Supabase URL or Key could not be found.")
        return

    supabase: Client = create_client(url, key)

    try:
        response = supabase.table("neighbor").select("*").limit(1).execute()
        print("Successfull! Supabase remains active.")
    except Exception as e:
        print(f"An error occured: {e}")

if __name__ == "__main__":
    main()