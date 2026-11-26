from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import threading
import unittest
import uuid
from tools.operations_worker import run

class OperationsWorkerTests(unittest.TestCase):
    def test_bounded_replay_and_fail_closed_protocol(self):
        calls=[]
        class Handler(BaseHTTPRequestHandler):
            def log_message(self,*args):pass
            def do_POST(self):
                calls.append(self.path)
                value={'exported':len(calls)<3}
                content=json.dumps(value).encode();self.send_response(200);self.end_headers();self.wfile.write(content)
        server=ThreadingHTTPServer(('127.0.0.1',0),Handler);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
        try:
            base='http://127.0.0.1:'+str(server.server_port);tenant=str(uuid.uuid4())
            self.assertEqual(2,run(base,tenant,'synthetic-token','mlflow',5));self.assertEqual(3,len(calls))
            self.assertTrue(all(p.endswith('/learning/mlflow/export') for p in calls))
            with self.assertRaises(ValueError):run(base,tenant,'synthetic-token','reports-purge',1)
            with self.assertRaises(ValueError):run(base+'/redirect',tenant,'synthetic-token','mlflow',1)
            with self.assertRaises(ValueError):run(base,tenant,'synthetic-token','../../foreign',1)
        finally:server.shutdown();server.server_close();thread.join()
