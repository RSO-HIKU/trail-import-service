# Trail Import Service

The Trail Import Service processes GPX files to import hiking trail data into the Peaks-Hikes Service database with geospatial coordinates and metadata. It parses GPX format files, extracts trail geometry and attributes, and persists them to PostgreSQL with PostGIS spatial extensions for geospatial queries. The service automatically links imported trails to nearby peaks based on proximity calculations, creating a comprehensive geographic database for the HIKU hiking application.

For full documentation see: [Docs](./documentation.md)
