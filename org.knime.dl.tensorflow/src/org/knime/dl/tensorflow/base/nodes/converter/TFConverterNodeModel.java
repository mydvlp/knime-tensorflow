/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.dl.tensorflow.base.nodes.converter;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.tensorflow.base.portobjects.TFNetworkPortObject;
import org.knime.dl.tensorflow.base.portobjects.TFNetworkPortObjectSpec;
import org.knime.dl.tensorflow.core.TFNetwork;
import org.knime.dl.tensorflow.core.TFNetworkSpec;
import org.knime.dl.tensorflow.core.convert.TFModelConverter;
import org.knime.dl.tensorflow.core.convert.TFModelConverterRegistry;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class TFConverterNodeModel extends NodeModel {

	private static final TFModelConverterRegistry CONVERTER_REGISTRY = TFModelConverterRegistry.getInstance();

	private TFModelConverter m_converter;

	/**
	 * Creates a new {@link NodeModel} for the TensorFlow Network Converter.
	 */
	protected TFConverterNodeModel() {
		super(new PortType[] { DLNetworkPortObject.TYPE }, new PortType[] { TFNetworkPortObject.TYPE });
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		final DLNetworkPortObjectSpec spec = (DLNetworkPortObjectSpec) inSpecs[0];
		final Class<? extends DLNetwork> networkType = spec.getNetworkType();
		final DLNetworkSpec networkSpec = spec.getNetworkSpec();

		// Get the correct converter
		m_converter = CONVERTER_REGISTRY.getConverter(networkSpec.getClass(), networkType);
		if (m_converter == null) {
			throw new InvalidSettingsException("No converter for the given network type \"" + networkType + "\" found.");
		}

		final TFNetworkSpec tfSpec = m_converter.convertSpec(networkSpec);
		if (tfSpec == null) {
			return new PortObjectSpec[] {};
		} else {
			return new PortObjectSpec[] { new TFNetworkPortObjectSpec(tfSpec, m_converter.getOutputNetworkType()) };
		}
	}

	@Override
	protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
		final DLNetworkPortObject in = (DLNetworkPortObject) inObjects[0];

		// Convert the network
		final FileStore fileStore = DLNetworkPortObject.createFileStoreForSaving(null, exec);
		final TFNetwork tfNetwork = m_converter.convertNetwork(in.getNetwork(), fileStore);

		return new PortObject[] { new TFNetworkPortObject(tfNetwork, fileStore) };
	}

	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// nothing to do
	}

	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// nothing to do
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		// nothing to do
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		// nothing to do
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		// nothing to do
	}

	@Override
	protected void reset() {
		m_converter = null;
	}
}
