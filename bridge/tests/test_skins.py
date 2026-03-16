"""Tests for skin CRUD endpoints and storage module."""
from __future__ import annotations

import json
import tempfile
from pathlib import Path
from unittest.mock import patch

import pytest
from aiohttp.test_utils import AioHTTPTestCase, unittest_run_loop

from bridge.server import create_app


class TestSkinEndpoints(AioHTTPTestCase):
    """Integration tests for /skins endpoints."""

    async def get_application(self):
        return create_app(enable_mdns=False)

    def setUp(self):
        # Use a temp dir for skin storage so tests don't pollute ~/.claude
        self._tmp = tempfile.mkdtemp()
        self._patcher = patch("bridge.skins.SKINS_DIR", Path(self._tmp))
        self._patcher.start()
        super().setUp()

    def tearDown(self):
        super().tearDown()
        self._patcher.stop()
        import shutil
        shutil.rmtree(self._tmp, ignore_errors=True)

    @unittest_run_loop
    async def test_list_skins_empty(self):
        resp = await self.client.request("GET", "/skins")
        assert resp.status == 200
        data = await resp.json()
        assert isinstance(data, list)
        assert len(data) == 0

    @unittest_run_loop
    async def test_upload_and_get_skin(self):
        skin = {"id": "test_neon", "name": "Neon Ghost", "description": "Neon colors"}
        resp = await self.client.request("POST", "/skins", json=skin)
        assert resp.status == 201
        body = await resp.json()
        assert body["accepted"] is True

        resp = await self.client.request("GET", "/skins/test_neon")
        assert resp.status == 200
        data = await resp.json()
        assert data["name"] == "Neon Ghost"

    @unittest_run_loop
    async def test_list_after_upload(self):
        skin = {"id": "listed", "name": "Listed Skin", "author": "tester"}
        await self.client.request("POST", "/skins", json=skin)

        resp = await self.client.request("GET", "/skins")
        data = await resp.json()
        assert len(data) == 1
        assert data[0]["id"] == "listed"
        assert data[0]["author"] == "tester"

    @unittest_run_loop
    async def test_get_nonexistent_skin(self):
        resp = await self.client.request("GET", "/skins/doesnotexist")
        assert resp.status == 404

    @unittest_run_loop
    async def test_delete_skin(self):
        skin = {"id": "deleteme", "name": "Delete Me"}
        await self.client.request("POST", "/skins", json=skin)
        resp = await self.client.request("DELETE", "/skins/deleteme")
        assert resp.status == 200

        resp = await self.client.request("GET", "/skins/deleteme")
        assert resp.status == 404

    @unittest_run_loop
    async def test_cannot_delete_ghost(self):
        resp = await self.client.request("DELETE", "/skins/ghost")
        assert resp.status == 404

    @unittest_run_loop
    async def test_upload_missing_id(self):
        resp = await self.client.request("POST", "/skins", json={"name": "No ID"})
        assert resp.status == 400

    @unittest_run_loop
    async def test_upload_invalid_json(self):
        resp = await self.client.request(
            "POST", "/skins",
            data="not json",
            headers={"Content-Type": "application/json"},
        )
        assert resp.status == 400
