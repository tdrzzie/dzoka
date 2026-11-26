import unittest
from tools.advisory_inventory import inventory

class AdvisoryInventoryTests(unittest.TestCase):
    def test_pagination_and_incomplete_responses_cannot_pass(self):
        lock={'dependencies':[{'coordinate':'org.example:fixture:1.0','scope':'compile'}]};calls=[]
        def fetch(body):
            calls.append(body)
            return {'results':[{'vulns':[{'id':'TEST-1','modified':'2026-01-01'}],'next_page_token':'next'}]} if len(calls)==1 else {'results':[{'vulns':[{'id':'TEST-2'}]}]}
        result=inventory(lock,fetch);self.assertEqual(2,len(result[0]['advisories']));self.assertEqual('next',calls[1]['queries'][0]['page_token'])
        with self.assertRaises(ValueError):inventory(lock,lambda body:{'results':[]})
        with self.assertRaises(ValueError):inventory(lock,lambda body:{'results':[{'error':'not found'}]})
